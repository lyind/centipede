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

    console.log("[profile-fragment] init");
    (function(app, document)
    {
        // draw profile fragment button on load
        app.renderKeepable("profile.fragment", function(keepable)
        {
            var profile = document.createElement('div');
            profile.classList.add("profile");
            profile.textContent = "ADMIN";

            var profileLogout = document.createElement('button');
            profileLogout.textContent = "Logout";
            profileLogout.onclick = function()
            {
                alert("Logout clicked!");
                return false;
            };

            var profileNode = keepable.appendChild(profile);
            profileNode.appendChild(profileLogout);

            console.log("[profile-fragment] rendered");
        });
    })(window.app, document);

});