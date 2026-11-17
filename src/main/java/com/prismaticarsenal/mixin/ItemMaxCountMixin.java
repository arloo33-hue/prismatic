package com.prismaticarsenal.mixin;

import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Every item without durability stacks to 64, including totems, ender pearls, potions, buckets and signs.
// (1.20.1 sends stack sizes over the network as one byte, so 64 is the safe maximum.)
@Mixin(Item.class)
public abstract class ItemMaxCountMixin {
    @Inject(method = "getMaxCount", at = @At("RETURN"), cancellable = true)
    private void prismaticArsenal$stackTo64(CallbackInfoReturnable<Integer> cir) {
        Item self = (Item) (Object) this;
        if (cir.getReturnValueI() < 64 && !self.isDamageable()) cir.setReturnValue(64);
    }
}
