/*
 * Copyright (C) 2017  Jonas Zeiger <jonas.zeiger@talpidae.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.talpidae.centipede.util.interpolation;

import org.bigtesting.interpolatd.Interpolator;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.val;

import static com.google.common.base.Strings.nullToEmpty;


/**
 * Allow usage of environment variables plus some custom variables in arguments and vmArguments.
 * <p>
 * Custom variables:
 * <p>
 * QUEEN_SOCKET  - insect queen host:port, example: 127.0.0.1:13300
 */
@Singleton
public class CommandLineInterpolator
{
    private final static int MAXIMUM_INTERPOLATION_DEPTH = 3;

    private final Interpolator<Map<String, String>> interpolator;


    @Inject
    public CommandLineInterpolator()
    {
        // setup interpolator
        val interpolator = new Interpolator<Map<String, String>>();
        interpolator.when()
                .enclosedBy("${").and("}")
                .handleWith((captured, arg) -> arg.get(captured));

        this.interpolator = interpolator;
    }


    /**
     * Interpolate a template string replacing variables of the form ${FNORD} with their corresponding instances in the environment and @param additionalVariables.
     */
    public String interpolate(String template, Map<String, String> environment)
    {
        String nonNullTemplate = nullToEmpty(template);

        // interpolate at most three levels
        int i = 0;
        String interpolated = nonNullTemplate;
        do
        {
            ++i;
            nonNullTemplate = interpolated;
            interpolated = interpolator.interpolate(nonNullTemplate, environment);
        }
        while (i < MAXIMUM_INTERPOLATION_DEPTH && !nonNullTemplate.equals(interpolated));

        return interpolated;
    }
}