package dev.anvilcraft.pigeonplus.integration.jei.category;

import dev.anvilcraft.pigeonplus.integration.jei.AnvilCraftPigeonPlusJeiPlugin;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionBrewingJeiRecipes;
import dev.anvilcraft.pigeonplus.integration.jei.recipe.PotionSoakingJeiRecipe;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 泡药：把物品浸泡在药水里，注入对应的药水效果。
 */
public class PotionSoakingCategory implements IRecipeCategory<PotionSoakingJeiRecipe> {
    private static final int WIDTH = 162;
    private static final int HEIGHT = 64;
    private static final int DISPLAY_AMOUNT = 1000;

    // 与本体 AbstractLiquidCategory 的小锅分支一致，只是把炼药锅换成鱼缸。
    private static final float MODEL_SCALE = 12.0F;
    private static final int ANVIL_Y = 12;
    private static final int TANK_Y = 30;
    private static final int ARROW_IN_X = 54;
    private static final int ARROW_OUT_X = 92;
    private static final int ARROW_Y = 22;

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;
    private final ITickTimer timer;
    private final BlockState tank;

    public PotionSoakingCategory(IGuiHelper helper) {
        this.icon = new DrawableBlockStateIcon(
            Blocks.ANVIL.defaultBlockState(),
            ModBlocks.FISH_TANK.getDefaultState()
        );
        this.slot = JeiRenderHelper.getSlotDefault(helper);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
        this.timer = helper.createTickTimer(30, 60, true);
        // 鱼缸的模型是 0..16 的满方块，与炼药锅同尺寸，所以沿用同一组坐标。
        this.tank = ModBlocks.FISH_TANK.getDefaultState();
    }

    @Override
    public RecipeType<PotionSoakingJeiRecipe> getRecipeType() {
        return AnvilCraftPigeonPlusJeiPlugin.POTION_SOAKING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.anvilcraft_pigeon_plus.category.potion_soaking");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        PotionSoakingJeiRecipe recipe,
        IFocusGroup focuses
    ) {
        // 物品在上、药水在下，与本体「有物品输入时物品上移、流体下移」的排法一致。
        builder.addSlot(RecipeIngredientRole.INPUT, JeiSlotUtil.INPUT_X, JeiSlotUtil.ITEM_Y)
            .addItemStack(recipe.input());
        IRecipeSlotBuilder potionSlot = JeiFluidUtil.addFluidSlot(
            builder,
            RecipeIngredientRole.INPUT,
            JeiSlotUtil.INPUT_X,
            JeiSlotUtil.FLUID_Y,
            16,
            16,
            DISPLAY_AMOUNT,
            false,
            recipe.potions()
        );
        IRecipeSlotBuilder resultSlot = builder.addSlot(
                RecipeIngredientRole.OUTPUT,
                JeiSlotUtil.OUTPUT_X,
                JeiSlotUtil.DEFAULT_Y
            )
            .addItemStacks(recipe.results());
        // 药水与产物按索引一一对应，联动让两边同步切换。
        if (recipe.potions().size() > 1) {
            builder.createFocusLink(potionSlot, resultSlot);
        }
        // 药水流体没有桶，补上药水物品让玩家拿药水能查到泡药配方。
        //
        // 两个角色都要登记：JEI 的 R 只搜产出、U 只搜输入。
        // 药水在泡药里是输入，只登记输入角色的话，对着药水按 R 是查不到这条配方的。
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
            .addItemStacks(PotionBrewingJeiRecipes.potionItemsOf(recipe.potions()));
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
            .addItemStacks(PotionBrewingJeiRecipes.potionItemsOf(recipe.potions()));
        // 产物同理：拿泡好的物品按 U 要能查到它的来源。
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
            .addItemStacks(recipe.results());
    }

    @Override
    public void draw(
        PotionSoakingJeiRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(
            guiGraphics, Blocks.ANVIL.defaultBlockState(), 81, ANVIL_Y + anvilYOffset, 20,
            MODEL_SCALE, RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(
            guiGraphics, this.tank, 81, TANK_Y, 10, MODEL_SCALE, RenderSupport.SINGLE_BLOCK);
        this.arrowIn.draw(guiGraphics, ARROW_IN_X, ARROW_Y);
        this.arrowOut.draw(guiGraphics, ARROW_OUT_X, ARROW_Y);

        this.slot.draw(guiGraphics, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.ITEM_Y - 1);
        this.slot.draw(guiGraphics, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.FLUID_Y - 1);
        this.slot.draw(guiGraphics, JeiSlotUtil.OUTPUT_X - 1, JeiSlotUtil.DEFAULT_Y - 1);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftPigeonPlusJeiPlugin.POTION_SOAKING,
            PotionBrewingJeiRecipes.buildSoaking(DISPLAY_AMOUNT)
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(
            registration, AnvilCraftPigeonPlusJeiPlugin.POTION_SOAKING);
        registration.addRecipeCatalyst(
            new ItemStack(Items.CAULDRON), AnvilCraftPigeonPlusJeiPlugin.POTION_SOAKING);
        registration.addRecipeCatalyst(
            new ItemStack(ModBlocks.FISH_TANK), AnvilCraftPigeonPlusJeiPlugin.POTION_SOAKING);
    }
}
