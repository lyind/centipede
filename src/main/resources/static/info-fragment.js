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
"use strict";

// persistent info element
app.require([
    "core/ui.js",
],
function()
{
    (function(app, document)
    {
        var renderInfo = function()
        {
            var test = document.createElement('p');

            test.setAttribute("keep", "info-container");
            test.onclick = function()
            {
                alert("Test area clicked!");
                return false;
            };
            test.textContent = "Some permanent thing (test click event)";

            var body = document.getElementsByTagName("body");
            if ((!!body.length))
            {
                body[0].appendChild(test);
            }
        };

        // draw info fragment button on load
        window.addEventListener('load', function()
        {
            renderInfo();
        });

    })(window.app, document);

});