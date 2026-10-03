package com.craftulator.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookComponent.class)
public class RecipeBookSearchMixin {

@Shadow
private EditBox searchBox;

@Shadow
protected Minecraft minecraft;

private String craftulator$lastText = "";
private String craftulator$resultText = "";

private void craftulator$checkCalculator() {
    if (searchBox == null) {
        return;
    }

    String input = searchBox.getValue().trim();

    if (input.equals(craftulator$lastText)) {
        return;
    }

    craftulator$lastText = input;
    craftulator$resultText = "";

    if (input.isEmpty()) {
        return;
    }

    // Normal Minecraft aramalarında hesaplama yapma.
    if (!craftulator$isCalculatorExpression(input)) {
        return;
    }

    try {
        double result = calculate(input);

        if (!Double.isFinite(result)) {
            return;
        }

        if (result == (long) result) {
            craftulator$resultText = String.valueOf((long) result);
        } else {
            craftulator$resultText = String.valueOf(result);
        }

    } catch (Exception ignored) {
        craftulator$resultText = "";
    }
}

private boolean craftulator$isCalculatorExpression(String input) {
    boolean hasOperator = false;

    for (char c : input.toCharArray()) {

        if (
                c == '+'
                        || c == '*'
                        || c == '/'
                        || c == '('
                        || c == ')'
        ) {
            hasOperator = true;
        }

        if (
                !(Character.isDigit(c)
                        || c == '.'
                        || c == '+'
                        || c == '-'
                        || c == '*'
                        || c == '/'
                        || c == '('
                        || c == ')'
                        || Character.isWhitespace(c))
        ) {
            return false;
        }
    }

    return hasOperator || input.matches(".*\\d+\\s*-\\s*\\d+.*");
}

private double calculate(String input) {
    final String expression = input.replace(" ", "");

    return new Object() {

        int position = -1;
        int character;

        void nextCharacter() {
            position++;

            character = position < expression.length()
                    ? expression.charAt(position)
                    : -1;
        }

        boolean eat(int characterToEat) {
            while (character == ' ') {
                nextCharacter();
            }

            if (character == characterToEat) {
                nextCharacter();
                return true;
            }

            return false;
        }

        double parse() {
            nextCharacter();

            double result = parseExpression();

            if (position < expression.length()) {
                throw new RuntimeException("Unexpected character");
            }

            return result;
        }

        double parseExpression() {
            double result = parseTerm();

            while (true) {
                if (eat('+')) {
                    result += parseTerm();
                } else if (eat('-')) {
                    result -= parseTerm();
                } else {
                    return result;
                }
            }
        }

        double parseTerm() {
            double result = parseFactor();

            while (true) {
                if (eat('*')) {
                    result *= parseFactor();
                } else if (eat('/')) {
                    result /= parseFactor();
                } else {
                    return result;
                }
            }
        }

        double parseFactor() {

            if (eat('+')) {
                return parseFactor();
            }

            if (eat('-')) {
                return -parseFactor();
            }

            double result;

            int startPosition = position;

            if (eat('(')) {

                result = parseExpression();

                if (!eat(')')) {
                    throw new RuntimeException("Missing ')'");
                }

            } else {

                while (
                        (character >= '0' && character <= '9')
                                || character == '.'
                ) {
                    nextCharacter();
                }

                if (startPosition == position) {
                    throw new RuntimeException("Expected number");
                }

                result = Double.parseDouble(
                        expression.substring(
                                startPosition,
                                position
                        )
                );
            }

            return result;
        }

    }.parse();
}

@Inject(
        method = "tick",
        at = @At("TAIL")
)
private void craftulator$onTick(CallbackInfo ci) {
    craftulator$checkCalculator();
}

@Inject(
        method = "extractRenderState",
        at = @At("TAIL")
)
private void craftulator$renderResult(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick,
        CallbackInfo ci
) {
    if (searchBox == null) {
        return;
    }

    if (!searchBox.isVisible()) {
        return;
    }

    if (craftulator$resultText.isEmpty()) {
        return;
    }

    graphics.nextStratum();

    int x = searchBox.getX() + 5;
    int y = searchBox.getY() + searchBox.getHeight() + 4;

    graphics.textRenderer().accept(
            x,
            y,
            Component.literal("= " + craftulator$resultText)
    );
}

}
