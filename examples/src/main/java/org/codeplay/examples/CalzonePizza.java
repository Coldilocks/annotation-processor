package org.codeplay.examples;

import org.codeplay.annotation.Factory;

/**
 * @author coldilock
 */
@Factory(id = "Calzone", type = Meal.class)
public class CalzonePizza implements Meal{
    @Override
    public float getPrice() {
        return 8.5f;
    }
}
