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
			NBTTagCompound currentStack;
			EntityPlayerMP player = ctx.getServerHandler().player;
			Container con = player.openContainer;
			ItemStack[] recipe = new ItemStack[9];
			ItemStack[] recipeOutput = null;
			for (int i = 0; i < recipe.length; ++i) {
				currentStack = (NBTTagCompound) message.input.getTag("#" + i);
				recipe[i] = currentStack == null ? ItemStack.EMPTY : new ItemStack(currentStack);
			}
			if (message.output != null) {
				recipeOutput = new ItemStack[3];
				NBTTagList outputList = message.output.getTagList(RecipeTransferHandler.OUTPUTS_KEY, 10);
				for (int i = 0; i < recipeOutput.length; ++i) {
					recipeOutput[i] = new ItemStack(outputList.getCompoundTagAt(i));
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
						for (int i = 0; i < craftMatrix.getSlots(); ++i) {
							ItemStack currentItem = ItemStack.EMPTY;
							if (recipe[i] != null) {
								currentItem = recipe[i].copy();
							}
							ItemHandlerUtil.setStackInSlot(craftMatrix, i, currentItem);
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
