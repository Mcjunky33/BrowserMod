package de.mcjunky33.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatComponent.class)
public abstract class ChatLinkMixin {

    @Unique
    private static final Pattern URL_PATTERN = Pattern.compile(
            "((?:https?://|www\\.)[\\w#$-=@&~.!\\*\\(\\),?]{1,256}\\.[a-z]{2,6}\\b[\\w#$-=@&~.!\\*\\(\\),+/?]*)",
            Pattern.CASE_INSENSITIVE
    );

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"), argsOnly = true)
    private Component onAddMessage(Component component) {
        if (component == null) return null;
        return transform(component);
    }

    @Unique
    private Component transform(Component component) {
        String text = component.getString();
        Matcher matcher = URL_PATTERN.matcher(text);
        if (!matcher.find()) return component;

        MutableComponent result = Component.empty();
        matcher.reset();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(Component.literal(text.substring(lastEnd, matcher.start())).withStyle(component.getStyle()));

            String url = matcher.group();
            String destination = url.startsWith("http") ? url : "https://" + url;

            ClickEvent linkClickEvent = null;
            try {
                linkClickEvent = new ClickEvent.OpenUrl(new URI(destination));
            } catch (Exception e) {
            }

            final ClickEvent finalEvent = linkClickEvent;

            result.append(Component.literal(url).withStyle(style -> {
                Style s = style.withColor(ChatFormatting.BLUE).withUnderlined(true);
                return finalEvent != null ? s.withClickEvent(finalEvent) : s;
            }));

            lastEnd = matcher.end();
        }


        result.append(Component.literal(text.substring(lastEnd)).withStyle(component.getStyle()));
        return result;
    }
}