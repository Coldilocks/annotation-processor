package org.codeplay.examples;

import org.codeplay.annotations.Factory;

/**
 * @author coldilock
 */
@Factory(id = "Tiramisu", type = Meal.class)
public class Tiramisu implements Meal{
    @Override
    public float getPrice() {
        return 4.5f;
    }
}
