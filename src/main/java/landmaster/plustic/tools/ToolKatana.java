package landmaster.plustic.tools;

import java.awt.Color;
import java.util.*;

import javax.annotation.*;

import landmaster.plustic.config.*;
import landmaster.plustic.util.*;
import net.minecraft.client.*;
import net.minecraft.client.resources.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.*;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.relauncher.*;
import slimeknights.tconstruct.library.materials.*;
import slimeknights.tconstruct.library.tinkering.*;
import slimeknights.tconstruct.library.tools.*;
import slimeknights.tconstruct.library.utils.*;
import slimeknights.tconstruct.tools.*;
import slimeknights.tconstruct.tools.ranged.item.*;

public class ToolKatana extends SwordCore {
	public static final float DURABILITY_MODIFIER = 0.88f;
	
	public static final String COUNTER_TAG = "PlusTiC_Counter";
	
	static {
		MinecraftForge.EVENT_BUS.register(ToolKatana.class);
	}
	
	private static float counter_multiplier(float attack) {
		if (attack <= 5) {
			return 1.2f;
		}
		if (attack <= 11) {
			return 1.35f;
		}
		return 1.5f;
	}
	
	public static float counter_cap(ItemStack tool) {
		float attack = TagUtil.getToolStats(tool).attack;
		return attack * counter_multiplier(attack);
	}
	
	public ToolKatana() {
		super(PartMaterialType.handle(TinkerTools.toughToolRod),
				PartMaterialType.head(TinkerTools.largeSwordBlade),
				PartMaterialType.head(TinkerTools.largeSwordBlade),
				PartMaterialType.extra(TinkerTools.toughBinding));
		setUnlocalizedName("katana").setRegistryName("katana");
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public static void render(RenderGameOverlayEvent event) {
		final Minecraft mc = Minecraft.getMinecraft();
		final ItemStack is = mc.thePlayer.getHeldItemMainhand();
		if (event.getType() == RenderGameOverlayEvent.ElementType.TEXT
				&& is != null && is.getItem() instanceof ToolKatana) {
			float counter = TagUtil.getTagSafe(is).getFloat(COUNTER_TAG);
			if (counter > 0) {
				mc.fontRendererObj.drawString(I18n.format("meter.plustic.katana", counter),
						5, 5, Color.HSBtoRGB(Math.min(counter/(counter_cap(is)*3), 1.0f/3), 1, 1) & 0xFFFFFF, true);
			}
		}
	}
	
	@Nonnull
	@Override
	public ActionResult<ItemStack> onItemRightClick(@Nonnull ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, EnumHand hand) {
		if (hand == EnumHand.MAIN_HAND && playerIn.getHeldItemOffhand() != null && !(playerIn.getHeldItemOffhand().getItem() instanceof Shuriken)) {
			return ActionResult.newResult(EnumActionResult.SUCCESS, itemStackIn);
		}
		return super.onItemRightClick(itemStackIn, worldIn, playerIn, hand);
	}
	
	@Override
	public int[] getRepairParts() {
		return new int[] {1,2};
	}
	
	@Override
	public double attackSpeed() {
		return 2.55;
	}

	@Override
	public float damagePotential() {
		return 0.77f;
	}
	
	@Override
	public float damageCutoff() {
		return 22.0f;
	}
	
	@Override
	public float knockback() {
		return 0.83f;
	}
	
	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
		super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);
		NBTTagCompound tag = TagUtil.getTagSafe(stack);
		float counter = tag.getFloat(COUNTER_TAG);
		counter -= 0.005f;
		counter = Utils.clamp(counter, 0, counter_cap(stack));
		tag.setFloat(COUNTER_TAG, counter);
		stack.setTagCompound(tag);
	}
	
	@Override
	public boolean dealDamage(ItemStack stack, EntityLivingBase player, Entity entity, float damage) {
		if (entity instanceof EntityLivingBase) {
			EntityLivingBase targetLiving = (EntityLivingBase)entity;
			if (targetLiving.getTotalArmorValue() <= 0) {
				damage += 2.6f; // increase damage against unarmored
			}
		}
		NBTTagCompound tag = TagUtil.getTagSafe(stack);
		float counter = tag.getFloat(COUNTER_TAG);
		damage += counter * Config.katana_combo_multiplier;
		
		boolean success = super.dealDamage(stack, player, entity, damage);
		if (success) {
			if (entity instanceof EntityLivingBase) {
				EntityLivingBase targetLiving = (EntityLivingBase)entity;
				if (targetLiving.getHealth() <= 0) counter += 1.0f;
				counter = Utils.clamp(counter, 0, counter_cap(stack));
			}
			tag.setFloat(COUNTER_TAG, counter);
			stack.setTagCompound(tag);
		}
		return success;
	}
	
	@Override
	public float getRepairModifierForPart(int index) {
	    return DURABILITY_MODIFIER;
	}

	@Override
	protected ToolNBT buildTagData(List<Material> materials) {
		ToolNBT data = buildDefaultTag(materials);
		data.attack += 1f;
		data.durability *= DURABILITY_MODIFIER;
		return data;
	}
	/*
	private static final Field lastDamageF;
	static {
		try {
			lastDamageF = EntityLivingBase.class.getDeclaredField(
					"field_110153_bc"); // lastDamage
			lastDamageF.setAccessible(true);
		} catch (Throwable e) {
			throw Throwables.propagate(e);
		}
	}
	
	private static float lastDamage(EntityLivingBase elb) {
		try {
			return lastDamageF.getFloat(elb);
		} catch (Throwable e) {
			throw Throwables.propagate(e);
		}
	}*/
}