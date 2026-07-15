package com.dinesh.geotaskai.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TaskValidatorTest {
    @Test
    public void latitude_acceptsWorldBoundsOnly() {
        assertTrue(TaskValidator.isValidLatitude(-90.0));
        assertTrue(TaskValidator.isValidLatitude(0.0));
        assertTrue(TaskValidator.isValidLatitude(90.0));

        assertFalse(TaskValidator.isValidLatitude(-90.1));
        assertFalse(TaskValidator.isValidLatitude(90.1));
        assertFalse(TaskValidator.isValidLatitude(Double.NaN));
        assertFalse(TaskValidator.isValidLatitude(Double.POSITIVE_INFINITY));
        assertFalse(TaskValidator.isValidLatitude(null));
    }

    @Test
    public void longitude_acceptsWorldBoundsOnly() {
        assertTrue(TaskValidator.isValidLongitude(-180.0));
        assertTrue(TaskValidator.isValidLongitude(0.0));
        assertTrue(TaskValidator.isValidLongitude(180.0));

        assertFalse(TaskValidator.isValidLongitude(-180.1));
        assertFalse(TaskValidator.isValidLongitude(180.1));
        assertFalse(TaskValidator.isValidLongitude(Double.NaN));
        assertFalse(TaskValidator.isValidLongitude(Double.NEGATIVE_INFINITY));
        assertFalse(TaskValidator.isValidLongitude(null));
    }

    @Test
    public void radius_requiresPositiveFiniteValue() {
        assertTrue(TaskValidator.isValidRadius(1.0));
        assertTrue(TaskValidator.isValidRadius(150.5));

        assertFalse(TaskValidator.isValidRadius(0.0));
        assertFalse(TaskValidator.isValidRadius(-1.0));
        assertFalse(TaskValidator.isValidRadius(Double.NaN));
        assertFalse(TaskValidator.isValidRadius(Double.POSITIVE_INFINITY));
        assertFalse(TaskValidator.isValidRadius(null));
    }

    @Test
    public void taskInputValidation_requiresTitleLocationAndValidLocationFields() {
        TaskInput validInput = new TaskInput(
            "Submit assignment",
            "Submit assignment when reaching college.",
            "College",
            "Medium",
            12.9716,
            77.5946,
            200.0
        );

        assertTrue(TaskValidator.isValidTaskInput(validInput));
        assertFalse(TaskValidator.isValidTaskInput(new TaskInput("", "", "College", "Medium", 12.0, 77.0, 100.0)));
        assertFalse(TaskValidator.isValidTaskInput(new TaskInput("Buy milk", "", " ", "Low", 12.0, 77.0, 100.0)));
        assertFalse(TaskValidator.isValidTaskInput(new TaskInput("Buy milk", "", "Store", "Low", 91.0, 77.0, 100.0)));
        assertFalse(TaskValidator.isValidTaskInput(new TaskInput("Buy milk", "", "Store", "Low", 12.0, 181.0, 100.0)));
        assertFalse(TaskValidator.isValidTaskInput(new TaskInput("Buy milk", "", "Store", "Low", 12.0, 77.0, 0.0)));
    }
}
