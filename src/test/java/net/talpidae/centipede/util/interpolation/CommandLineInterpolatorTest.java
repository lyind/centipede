package net.talpidae.centipede.util.interpolation;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;


public class CommandLineInterpolatorTest
{
    private static final Map<String, String> testEnvironment = ImmutableMap.of(
            "KEY1", "hello ",
            "KEY2", "${KEY3}${KEY1}world",
            "KEY3", "${KEY1}"
    );


    @Test
    public void testInterpolate()
    {
        assertEquals(new CommandLineInterpolator().interpolate("Another ${KEY2}!", testEnvironment), "Another hello hello world!");
    }
}
