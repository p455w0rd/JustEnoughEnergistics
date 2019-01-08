package p455w0rd.jee.integration;

import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Table.Cell;

import appeng.container.implementations.ContainerPatternTerm;
import appeng.util.Platform;
import mezz.jei.api.*;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IModIngredientRegistration;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.collect.Table;
import mezz.jei.config.Constants;
import mezz.jei.recipes.RecipeTransferRegistry;
import mezz.jei.transfer.RecipeTransferErrorInternal;
import mezz.jei.transfer.RecipeTransferErrorTooltip;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import p455w0rd.jee.init.ModLogger;
import p455w0rd.jee.init.ModNetworking;
import p455w0rd.jee.packets.PacketJEIPatternRecipe;
import p455w0rd.jee.util.WrappedTable;

@SuppressWarnings("rawtypes")
@JEIPlugin
public class JEI implements IModPlugin {

	private static final IRecipeTransferError NEEDED_MODE_CRAFTING = new IncorrectTerminalModeError(true);
	private static final IRecipeTransferError NEEDED_MODE_PROCESSING = new IncorrectTerminalModeError(false);

	@Override
	public void register(@Nonnull IModRegistry registry) {
		Table<Class<?>, String, IRecipeTransferHandler> newRegistry = Table.hashBasedTable();
		boolean ae2found = false;
		for (Cell<Class, String, IRecipeTransferHandler> currentCell : ((RecipeTransferRegistry) registry.getRecipeTransferRegistry()).getRecipeTransferHandlers().cellSet()) {
			if (currentCell.getRowKey().equals(ContainerPatternTerm.class)) {
				ae2found = true;
				continue;
			}
			newRegistry.put(currentCell.getRowKey(), currentCell.getColumnKey(), currentCell.getValue());
		}
		newRegistry.put(ContainerPatternTerm.class, Constants.UNIVERSAL_RECIPE_TRANSFER_UID, new RecipeTransferHandler());
		if (ae2found) {
			ModLogger.info("AE2 RecipeTransferHandler Replaced Successfully (Registered prior)");
		}
		else {
			newRegistry = new WrappedTable<Class<?>, String, IRecipeTransferHandler>(newRegistry);
		}
		ReflectionHelper.setPrivateValue(RecipeTransferRegistry.class, ((RecipeTransferRegistry) registry.getRecipeTransferRegistry()), newRegistry, "recipeTransferHandlers");
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
	}

	@Override
	public void registerIngredients(IModIngredientRegistration registry) {
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistry subtypeRegistry) {
	}

	public static class RecipeTransferHandler implements IRecipeTransferHandler<ContainerPatternTerm> {

		public static final String OUTPUTS_KEY = "Outputs";

		@Override
		public Class<ContainerPatternTerm> getContainerClass() {
			return ContainerPatternTerm.class;
		}

		@Override
		@Nullable
		public IRecipeTransferError transferRecipe(ContainerPatternTerm container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
			String recipeType = recipeLayout.getRecipeCategory().getUid();
			if (doTransfer) {
				Map<Integer, ? extends IGuiIngredient<ItemStack>> ingredients = recipeLayout.getItemStacks().getGuiIngredients();
				NBTTagCompound recipeInputs = new NBTTagCompound();
				NBTTagCompound recipeOutputs = null;
				NBTTagList outputList = new NBTTagList();
				int inputIndex = 0;
				int outputIndex = 0;
				for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> ingredientEntry : ingredients.entrySet()) {
					IGuiIngredient<ItemStack> ingredient = ingredientEntry.getValue();
					if (ingredient == null || ingredient.getDisplayedIngredient() == null) {
						continue;
					}
					if (!ingredient.isInput()) {
						if (outputIndex >= 3 || container.isCraftingMode()) {
							continue;
						}
						outputList.appendTag(ingredient.getDisplayedIngredient().writeToNBT(new NBTTagCompound()));
						++outputIndex;
						continue;
					}
					for (Slot slot : container.inventorySlots) {
						if (slot.getSlotIndex() != inputIndex) {
							continue;
						}
						NBTTagList tags = new NBTTagList();
						List<ItemStack> list = new ArrayList<>();
						ItemStack displayed = ingredient.getDisplayedIngredient();
						if (displayed != null && !displayed.isEmpty()) {
							list.add(displayed);
						}
						for (ItemStack stack : ingredient.getAllIngredients()) {
							if (!Platform.isRecipePrioritized(stack)) {
								list.add(0, stack);
								continue;
							}
							list.add(stack);
						}
						for (ItemStack is : list) {
							NBTTagCompound tag = new NBTTagCompound();
							is.writeToNBT(tag);
							tags.appendTag(tag);
						}
						recipeInputs.setTag("#" + slot.getSlotIndex(), tags);
						break;
					}
					++inputIndex;
				}
				if (!outputList.hasNoTags()) {
					recipeOutputs = new NBTTagCompound();
					recipeOutputs.setTag(OUTPUTS_KEY, outputList);
				}
				ModNetworking.getInstance().sendToServer(new PacketJEIPatternRecipe(recipeInputs, recipeOutputs));
			}
			if (!recipeType.equals(VanillaRecipeCategoryUid.INFORMATION) && !recipeType.equals(VanillaRecipeCategoryUid.FUEL)) {
				if (!container.isCraftingMode()) {
					if (recipeType.equals(VanillaRecipeCategoryUid.CRAFTING)) {
						return NEEDED_MODE_CRAFTING;
					}
				}
				else if (!recipeType.equals(VanillaRecipeCategoryUid.CRAFTING)) {
					return NEEDED_MODE_PROCESSING;
				}
			}
			else {
				return RecipeTransferErrorInternal.INSTANCE;
			}
			return null;
		}
	}

	private static class IncorrectTerminalModeError extends RecipeTransferErrorTooltip {

		private static final String CRAFTING = I18n.format("tooltip.jee.crafting", new Object[0]);
		private static final String PROCESSING = I18n.format("tooltip.jee.processing", new Object[0]);

		public IncorrectTerminalModeError(boolean needsCrafting) {
			super(I18n.format("tooltip.jee.errormsg", TextFormatting.BOLD + (needsCrafting ? CRAFTING : PROCESSING) + TextFormatting.RESET + "" + TextFormatting.RED));
		}

	}

}
