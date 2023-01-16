package agency.highlysuspect.incorporeal.block.entity;

import agency.highlysuspect.incorporeal.platform.IncXplat;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.botania.api.BotaniaAPIClient;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.common.block.block_entity.BotaniaBlockEntity;

import java.util.Optional;

/**
 * Parent class of all "soul cores", handling the soul-grabbing part and some mana logistics.
 */
public abstract class AbstractSoulCoreBlockEntity extends BotaniaBlockEntity implements ManaReceiver {
	public AbstractSoulCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
	
	//idk where else to stick this
	public static final DamageSource SOUL = IncXplat.INSTANCE.createDamageSource("incorporeal.soul").setMagic();
	
	protected GameProfile ownerProfile;
	protected int mana;
	protected int signal;
	
	protected abstract int getMaxMana();
	protected abstract void tick();
	protected abstract int computeSignal();
	
	public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractSoulCoreBlockEntity self) {
		self.tick();
		
		int newSignal = self.computeSignal();
		if(self.signal != newSignal) {
			self.signal = newSignal;
			self.setChanged();
		}
		
		if(self.mana <= 0 && self.hasOwnerProfile()) {
			self.onExpire();
		}
	}
	
	public boolean hasOwnerProfile() {
		return ownerProfile != null;
	}
	
	public GameProfile getOwnerProfile() {
		return ownerProfile;
	}
	
	public void setOwnerProfile(GameProfile ownerProfile) {
		this.ownerProfile = ownerProfile;
		setChanged();
		VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
	}
	
	public Optional<ServerPlayer> findPlayer() {
		if(level == null || !hasOwnerProfile()) return Optional.empty();
		if(level.isClientSide()) throw new IllegalStateException("findPlayer on client level");
		
		MinecraftServer server = level.getServer();
		assert server != null;
		ServerPlayer player = server.getPlayerList().getPlayer(ownerProfile.getId());
		
		//Must be in the same dimension (gameplay limitation, not technical)
		if(player == null || player.level != level) return Optional.empty();
		else return Optional.of(player);
	}
	
	//Level (really EntityGetter)#getPlayerByUUID is slower than PlayerList#getPlayer when there are a lot of players.
	//PlayerList uses a proper uuid->player hashmap, but EntityGetter's uses a linear scan lol.
	//It's not that much slower. Actually i probably shouldn't worry about it. Being able to get ServerPlayers is a convenience tho.
	//This method doesn't have to check that the dimension is the same because it only queries the players on one Level.
	public Optional<Player> findPlayerClientSafeLol() {
		if(level == null || !hasOwnerProfile()) return Optional.empty();
		return Optional.ofNullable(level.getPlayerByUUID(ownerProfile.getId()));
	}
	
	public InteractionResult activate(Player player, InteractionHand hand) {
		assert level != null;
		
		if(!player.getGameProfile().equals(ownerProfile)) {
			//set the soul core to this player's profile
			setOwnerProfile(player.getGameProfile());
			
			if(!level.isClientSide()) {
				player.hurt(SOUL, 5f);
				receiveInitialMana();
			}
			
			return InteractionResult.SUCCESS;
		}
		
		return InteractionResult.PASS;
	}
	
	public void receiveInitialMana() {
		int n = getMaxMana() / 2;
		if(mana < n) mana = n;
		
		setChanged();
		VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
	}
	
	public void drainMana(int howMuch) {
		if(howMuch < 0) howMuch = 0;
		mana -= howMuch;
		if(mana < 0) mana = 0;
		
		setChanged();
		VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
	}
	
	public void onExpire() {
		findPlayerClientSafeLol().ifPresent(p -> p.hurt(SOUL, 5f));
		setOwnerProfile(null);
		if(level != null) level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, .5f, 1.2f);
	}
	
	public int getSignal() {
		return signal;
	}
	
	@Override
	public boolean isFull() {
		return mana >= getMaxMana();
	}
	
	@Override
	public void receiveMana(int moreMana) {
		//technically allow overfilling it...? see TileAvatar
		mana = Math.min(mana + moreMana, getMaxMana() * 2);
		
		setChanged();
		VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
	}
	
	@Override
	public boolean canReceiveManaFromBursts() {
		return true;
	}
	
	@Override
	public int getCurrentMana() {
		return mana;
	}
	
	@Override
	public void writePacketNBT(CompoundTag tag) {
		super.writePacketNBT(tag);
		
		if(ownerProfile != null) tag.put("OwnerProfile", NbtUtils.writeGameProfile(new CompoundTag(), ownerProfile));
		tag.putInt("Mana", mana);
		tag.putInt("Signal", signal);
	}
	
	@Override
	public void readPacketNBT(CompoundTag cmp) {
		super.readPacketNBT(cmp);
		
		if(cmp.contains("OwnerProfile")) ownerProfile = NbtUtils.readGameProfile(cmp.getCompound("OwnerProfile"));
		else ownerProfile = null;
		
		mana = cmp.getInt("Mana");
		signal = cmp.getInt("Signal");
	}
	
	@Override
	public Level getManaReceiverLevel() {
		return level;
	}
	
	@Override
	public BlockPos getManaReceiverPos() {
		return worldPosition;
	}
	
	public static class WandHud implements WandHUD {
		public WandHud(AbstractSoulCoreBlockEntity tile) {
			this.tile = tile;
		}
		
		private final AbstractSoulCoreBlockEntity tile;
		
		@Override
		public void renderHUD(PoseStack ms, Minecraft mc) {
			//copy from spreader lol
			String name = (new ItemStack(this.tile.getBlockState().getBlock())).getHoverName().getString();
			BotaniaAPIClient.instance().drawSimpleManaHUD(ms, 0xee4444, tile.mana, tile.getMaxMana(), name);
		}
	}
}
