package p455w0rd.jee.packets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.helpers.IContainerCraftingPacket;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.WrapperInvItemHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.items.IItemHandler;
import p455w0rd.jee.integration.JEI.RecipeTransferHandler;

public class PacketJEIPatternRecipe implements IMessage, IMessageHandler<PacketJEIPatternRecipe, IMessage> {

	NBTTagCompound input;
	NBTTagCompound output;

	public PacketJEIPatternRecipe() {
	}

	public PacketJEIPatternRecipe(@Nonnull NBTTagCompound input, @Nullable NBTTagCompound output) {
		this.input = input;
		this.output = output;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		input = ByteBufUtils.readTag(buf);
		if (buf.readBoolean()) {
			output = ByteBufUtils.readTag(buf);
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeTag(buf, input);
		if (output != null) {
			buf.writeBoolean(true);
			ByteBufUtils.writeTag(buf, output);
		}
		else {
			buf.writeBoolean(false);
		}
	}

	@Override
	public IMessage onMessage(PacketJEIPatternRecipe message, MessageContext ctx) {
		FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
			int x;
			NBTTagList list;
			EntityPlayerMP player = ctx.getServerHandler().player;
			Container con = player.openContainer;
			ItemStack[][] recipe = new ItemStack[9][];
			ItemStack[] recipeOutput = null;
			for (x = 0; x < recipe.length; ++x) {
				list = message.input.getTagList("#" + x, 10);
				if (list.tagCount() <= 0) {
					continue;
				}
				recipe[x] = new ItemStack[list.tagCount()];
				for (int y = 0; y < list.tagCount(); ++y) {
					recipe[x][y] = new ItemStack(list.getCompoundTagAt(y));
				}
			}
			if (message.output != null) {
				recipeOutput = new ItemStack[3];
				for (x = 0; x < recipeOutput.length; ++x) {
					list = message.output.getTagList(RecipeTransferHandler.OUTPUTS_KEY, 10);
					recipeOutput[x] = new ItemStack(list.getCompoundTagAt(x));
				}
			}
			if (con instanceof IContainerCraftingPacket && con instanceof ContainerPatternTerm) {
				IActionHost obj;
				IContainerCraftingPacket cct = (IContainerCraftingPacket) con;
				IGridNode node = cct.getNetworkNode();
				if (node == null && (obj = cct.getActionSource().machine().get()) != null) {
					node = obj.getActionableNode();
				}
				if (node != null) {
					IGrid grid = node.getGrid();
					if (grid == null) {
						return;
					}
					IStorageGrid inv = (IStorageGrid) grid.getCache(IStorageGrid.class);
					ISecurityGrid security = (ISecurityGrid) grid.getCache(ISecurityGrid.class);
					IItemHandler craftMatrix = cct.getInventoryByName("crafting");
					if (inv != null && recipe != null && security != null) {
						for (int x2 = 0; x2 < craftMatrix.getSlots(); ++x2) {
							ItemStack currentItem = ItemStack.EMPTY;
							if (recipe[x2] != null) {
								for (int y = 0; y < recipe[x2].length; ++y) {
									currentItem = recipe[x2][y].copy();
								}
							}
							ItemHandlerUtil.setStackInSlot(craftMatrix, x2, currentItem);
						}
						if (recipeOutput == null) {
							con.onCraftMatrixChanged(new WrapperInvItemHandler(craftMatrix));
						}
					}
					if (recipeOutput != null && !((ContainerPatternTerm) con).isCraftingMode()) {
						IItemHandler outputInv = cct.getInventoryByName("output");
						for (int i = 0; i < recipeOutput.length; ++i) {
							ItemHandlerUtil.setStackInSlot(outputInv, i, recipeOutput[i]);
						}
					}
				}
			}
		});
		return null;
	}

	@Nonnull
	private ItemStack canUseInSlot(int slot, ItemStack is, ItemStack[][] recipe) {
		if (recipe != null && recipe[slot] != null) {
			for (ItemStack stack : recipe[slot]) {
				if (!is.isItemEqual(stack)) {
					continue;
				}
				return is;
			}
		}
		return ItemStack.EMPTY;
	}
}
