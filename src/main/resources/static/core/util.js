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

// UI helper
app.require([],
function()
{
    console.log("[ui] init");

    (function(app, window)
    {
        // test for storage capability, example: app.storageAvailable("localStorage") -> true
        // copied from https://developer.mozilla.org/en-US/docs/Web/API/Web_Storage_API/Using_the_Web_Storage_API
        Object.defineProperty(app, "storageAvailable", { value: function(type)
        {
            try {
                var storage = window[type],
                    x = '__storage_test__';
                storage.setItem(x, x);
                storage.removeItem(x);
                return true;
            }
            catch(e) {
                return e instanceof DOMException && (
                    // everything except Firefox
                    e.code === 22 ||
                    // Firefox
                    e.code === 1014 ||
                    // test name field too, because code might not be present
                    // everything except Firefox
                    e.name === 'QuotaExceededError' ||
                    // Firefox
                    e.name === 'NS_ERROR_DOM_QUOTA_REACHED') &&
                    // acknowledge QuotaExceededError only if there's something already stored
                    storage.length !== 0;
            }
        }});

        // just a few shortcuts over using Observable.fromEvent() manually
        Object.defineProperty(app, "eachClick", { value: function(element)
        {
            return Rx.Observable.fromEvent(element, "click");
        }});


        Object.defineProperty(app, "eachSubmit", { value: function(element)
        {
            return Rx.Observable.fromEvent(element, "submit");
        }});

    })(window.app, window);
});