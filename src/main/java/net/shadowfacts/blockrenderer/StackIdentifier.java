package net.shadowfacts.blockrenderer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/**
 * @author shadowfacts
 */
@AllArgsConstructor
public class StackIdentifier {

	@Getter
	private ItemStack stack;

	public Item getItem() {
		return stack.getItem();
	}

	public int getMeta() {
		return stack.getMetadata();
	}

	public NBTTagCompound getTag() {
		return stack.getTagCompound();
	}

	public ResourceLocation getID() {
		return Item.REGISTRY.getNameForObject(getItem());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StackIdentifier that = (StackIdentifier) o;

		if (getMeta() != that.getMeta()) return false;
		if (!getItem().equals(that.getItem())) return false;
		return getTag() != null ? getTag().equals(that.getTag()) : that.getTag() == null;

	}

	@Override
	public int hashCode() {
		int result = getItem().hashCode();
		result = 31 * result + getMeta();
		result = 31 * result + (getTag() != null ? getTag().hashCode() : 0);
		return result;
	}

}
