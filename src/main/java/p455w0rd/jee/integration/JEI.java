package p455w0rd.jee.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Table.Cell;

import appeng.container.implementations.ContainerPatternTerm;
import appeng.util.Platform;
import mezz.jei.api.*;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IModIngredientRegistration;
import mezz.jei.api.recipe.IStackHelper;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.collect.Table;
import mezz.jei.config.Constants;
import mezz.jei.recipes.RecipeTransferRegistry;
import mezz.jei.startup.StackHelper;
import mezz.jei.transfer.RecipeTransferErrorInternal;
import mezz.jei.transfer.RecipeTransferErrorTooltip;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import p455w0rd.jee.init.ModLogger;
import p455w0rd.jee.init.ModNetworking;
import p455w0rd.jee.packets.PacketJEIPatternRecipe;
import p455w0rd.jee.util.WrappedTable;

@SuppressWarnings({
		"rawtypes", "deprecation"
})
@JEIPlugin
public class JEI implements IModPlugin {

	private static final IRecipeTransferError NEEDED_MODE_CRAFTING = new IncorrectTerminalModeError(true);
	private static final IRecipeTransferError NEEDED_MODE_PROCESSING = new IncorrectTerminalModeError(false);
	private static StackHelper stackHelper = null;

	@Override
	public void register(@Nonnull final IModRegistry registry) {
		final IStackHelper ish = registry.getJeiHelpers().getStackHelper();
		if (ish instanceof StackHelper) {
			stackHelper = (StackHelper) registry.getJeiHelpers().getStackHelper();
		}
		Table<Class<?>, String, IRecipeTransferHandler> newRegistry = Table.hashBasedTable();
		boolean ae2found = false;
		for (final Cell<Class, String, IRecipeTransferHandler> currentCell : ((RecipeTransferRegistry) registry.getRecipeTransferRegistry()).getRecipeTransferHandlers().cellSet()) {
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
			newRegistry = new WrappedTable<>(newRegistry);
		}
		ReflectionHelper.setPrivateValue(RecipeTransferRegistry.class, (RecipeTransferRegistry) registry.getRecipeTransferRegistry(), newRegistry, "recipeTransferHandlers");
	}

	@Override
	public void onRuntimeAvailable(final IJeiRuntime jeiRuntime) {
	}

	@Override
	public void registerIngredients(final IModIngredientRegistration registry) {
	}

	@Override
	public void registerItemSubtypes(final ISubtypeRegistry subtypeRegistry) {
	}

	public static StackHelper getStackHelper() {
		return stackHelper;
	}

	public static class RecipeTransferHandler implements IRecipeTransferHandler<ContainerPatternTerm> {

		public static final String OUTPUTS_KEY = "Outputs";

		@Override
		public Class<ContainerPatternTerm> getContainerClass() {
			return ContainerPatternTerm.class;
		}

		private Boolean isVanillaCrafting(final String recipeType, final ContainerPatternTerm container){
			if (!recipeType.equals(VanillaRecipeCategoryUid.INFORMATION) && !recipeType.equals(VanillaRecipeCategoryUid.FUEL)) {
				if (recipeType.equals(VanillaRecipeCategoryUid.CRAFTING)) {
					return Boolean.TRUE;
				}
			}
			return Boolean.FALSE;
		}

		@Override
		@Nullable
		public IRecipeTransferError transferRecipe(final ContainerPatternTerm container, final IRecipeLayout recipeLayout, final EntityPlayer player, final boolean maxTransfer, final boolean doTransfer) {
			final String recipeType = recipeLayout.getRecipeCategory().getUid();
			if (doTransfer) {
				final Map<Integer, ? extends IGuiIngredient<ItemStack>> ingredients = recipeLayout.getItemStacks().getGuiIngredients();
				final List<ItemStack> uniqueItems = new ArrayList<>();
				final NBTTagCompound recipeInputs = new NBTTagCompound();
				NBTTagCompound recipeOutputs = null;
				final NBTTagList outputList = new NBTTagList();
				int inputIndex = 0;
				int outputIndex = 0;
				for (final Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> ingredientEntry : ingredients.entrySet()) {
					final IGuiIngredient<ItemStack> guiIngredient = ingredientEntry.getValue();
					if (guiIngredient != null) {
						ItemStack ingredient = ItemStack.EMPTY;
						if (guiIngredient.getDisplayedIngredient() != null) {
							ingredient = guiIngredient.getDisplayedIngredient().copy();
						}

						if (guiIngredient.isInput()) {
							final List<ItemStack> currentList = guiIngredient.getAllIngredients();
							ItemStack stack = currentList.isEmpty() ? ItemStack.EMPTY : currentList.get(0);
							for (final ItemStack currentStack : currentList) {
								if (currentStack != null && Platform.isRecipePrioritized(currentStack)) {
									stack = currentStack.copy();
								}
							}
							if (stack == null) {
								stack = ItemStack.EMPTY;
							}

							if (isVanillaCrafting (recipeType, container)) {
								uniqueItems.add(stack);
							}
							else {
								if(!stack.isEmpty()) {
									ItemStack existingStack = ItemStack.EMPTY;
									for (ItemStack item : uniqueItems) {
										if (item.isItemEqual(stack) && (item.getCount() != item.getMaxStackSize())) {
											existingStack = item;
											break;
										}
									}
									if (existingStack.isEmpty()) {
										uniqueItems.add(stack.copy());
									} else {
										// limit stacks to max size as pattern can only support that.
										Integer maxStackSize = existingStack.getMaxStackSize();
										Integer newCount = existingStack.getCount() + stack.getCount();
										if (newCount > maxStackSize ) {
											existingStack.setCount(maxStackSize);
											stack.setCount(newCount - maxStackSize);
											uniqueItems.add(stack.copy());
										}
										else {
											existingStack.grow(stack.getCount());
										}
									}
								}
							}
						}
						else {
							if (outputIndex >= 3 || ingredient.isEmpty() || container.isCraftingMode()) {
								continue;
							}
							outputList.appendTag(ingredient.writeToNBT(new NBTTagCompound()));
							++outputIndex;
							continue;
						}
					}
				}
				for (ItemStack instack : uniqueItems) {
					recipeInputs.setTag("#" + inputIndex, instack.writeToNBT(new NBTTagCompound()));
					inputIndex++;
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

		private static final String CRAFTING = I18n.translateToLocalFormatted("tooltip.jee.crafting", new Object[0]);
		private static final String PROCESSING = I18n.translateToLocalFormatted("tooltip.jee.processing", new Object[0]);

		public IncorrectTerminalModeError(final boolean needsCrafting) {
			super(I18n.translateToLocalFormatted("tooltip.jee.errormsg", TextFormatting.BOLD + (needsCrafting ? CRAFTING : PROCESSING) + TextFormatting.RESET + "" + TextFormatting.RED));
		}

	}

}
