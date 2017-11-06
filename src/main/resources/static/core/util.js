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
    function ()
    {
        console.log("[util] init");

        (function (app, window)
        {
            // test for storage capability, example: app.storageAvailable("localStorage") -> true
            // copied from https://developer.mozilla.org/en-US/docs/Web/API/Web_Storage_API/Using_the_Web_Storage_API
            Object.defineProperty(app, "storageAvailable", {
                value: function (type)
                {
                    try
                    {
                        var storage = window[type],
                            x = '__storage_test__';
                        storage.setItem(x, x);
                        storage.removeItem(x);
                        return true;
                    }
                    catch (e)
                    {
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
                }
            });

            // simple storage abstraction (ignores quotas)
            var haveLocalStorage = app.storageAvailable("localStorage");
            if (haveLocalStorage || app.storageAvailable("sessionStorage"))
            {
                var storage = haveLocalStorage ? window.localStorage : window.sessionStorage;
                Object.defineProperty(app, "set", {
                    value: function (key, value)
                    {
                        storage.setItem(key, value);
                    }
                });
                Object.defineProperty(app, "get", {
                    value: function (key)
                    {
                        return storage.getItem(key);
                    }
                });
                Object.defineProperty(app, "remove", {
                    value: function (key)
                    {
                        storage.removeItem(key);
                    }
                });
            }
            else
            {
                var storage = {};
                Object.defineProperty(app, "set", {
                    value: function (key, value)
                    {
                        storage[key] = value;
                    }
                });
                Object.defineProperty(app, "get", {
                    value: function (key)
                    {
                        if (Object.prototype.hasOwnProperty.call(storage, key))
                        {
                            return storage[key];
                        }

                        return undefined;
                    }
                });
                Object.defineProperty(app, "remove", {
                    value: function (key)
                    {
                        if (Object.prototype.hasOwnProperty.call(fallbackStorage, key))
                        {
                            delete fallbackStorage[key];
                        }
                    }
                });
            }

            Object.defineProperty(app, "hide", {
                value: function (element)
                {
                    element.setAttribute("hidden", "");
                    element.classList.add("d-hide");
                }
            });

            Object.defineProperty(app, "show", {
                value: function (element)
                {
                    element.removeAttribute("hidden");
                    element.classList.remove("d-hide");
                }
            });

            Object.defineProperty(app, "isHidden", {
                value: function (element)
                {
                    return !element || element.hasAttribute("hidden") || element.classList.contains("d-hide");
                }
            });

            Object.defineProperty(app, "setReadonly", {
                value: function (element, isReadonly)
                {
                    var tagName = (element.tagName || "").toLowerCase();
                    if (isReadonly)
                    {
                        if (tagName === "select")
                        {
                            element.setAttribute("disabled", "");
                        }
                        else
                        {
                            element.setAttribute("readonly", "");
                        }
                    }
                    else
                    {
                        if (tagName === "select")
                        {
                            element.removeAttribute("disabled");
                        }
                        else
                        {
                            element.removeAttribute("readonly");
                        }
                    }
                }
            });

            Object.defineProperty(app, "setVisibility", {
                value: function (element, shallBeVisible)
                {
                    if (shallBeVisible)
                        app.show(element);
                    else
                        app.hide(element);
                }
            });

            // Test if a value is not "undefined" and not "null"
            Object.defineProperty(app, "isPresent", {
                value: function (value)
                {
                    return !!value;
                }
            });

            // Test if a value is an object
            Object.defineProperty(app, "isObject", {
                value: function (value)
                {
                    return value === Object(value);
                }
            });

            // Test if a value is a function (Date, Array, Object... are functions too)
            Object.defineProperty(app, "isFunction", {
                value: function (value)
                {
                    return typeof(value) === 'function';
                }
            });

            var stopEventPropagation = function (e)
            {
                e.preventDefault();
                e.stopPropagation();
            };

            // just a few shortcuts over using Observable.fromEvent() manually
            Object.defineProperty(app, "eachClick", {
                value: function (element)
                {
                    return Rx.Observable.fromEvent(element, "click").do(stopEventPropagation);
                }
            });

            Object.defineProperty(app, "eachClickUntilNavigate", {
                value: function (element)
                {
                    return app.eachClick(element)
                        .takeUntil(app.navigate());
                }
            });

            Object.defineProperty(app, "eachSubmit", {
                value: function (element)
                {
                    return Rx.Observable.fromEvent(element, "submit").do(stopEventPropagation);
                }
            });

            Object.defineProperty(app, "eachInput", {
                value: function (element)
                {
                    return Rx.Observable.fromEvent(element, "input");
                }
            });

            Object.defineProperty(app, "eachInputValue", {
                value: function (element)
                {
                    return app.eachInput(element)
                        .map(function (e)
                        {
                            return e.target.value;
                        });
                }
            });

            Object.defineProperty(app, "eachMouseOver", {
                value: function (element)
                {
                    return Rx.Observable.fromEvent(element, "mouseover").do(stopEventPropagation);
                }
            });

            Object.defineProperty(app, "eachChange", {
                value: function (element)
                {
                    return Rx.Observable.fromEvent(element, "change");
                }
            });

            Object.defineProperty(app, "eachLoad", {
                value: function (element)
                {
                    return Rx.Observable.fromEvent(element, "load");
                }
            });

            Object.defineProperty(app, "emptyToUndefined", {
                value: function (value)
                {
                    return (value) ? value : undefined;
                }
            });

            Object.defineProperty(app, "isNotEmpty", {
                value: function (arrayValue)
                {
                    return !!(arrayValue && arrayValue.length);
                }
            });

            Object.defineProperty(app, "uploadFile", {
                value: function (accept, multiple)
                {
                    var input = document.createElement('input');

                    if (accept !== undefined)
                        input.setAttribute('accept', accept);

                    if (multiple)
                        input.setAttribute('multiple', '');

                    input.setAttribute('type', 'file');

                    input.style.display = 'none';
                    input.setAttribute('id', 'file-input-hidden-element');
                    var inputNode = document.body.appendChild(input);

                    return Rx.Observable.empty()
                        .do(null, null, function ()
                        {
                            // completes immediately when the observable is subscribed to
                            inputNode.click();
                        })
                        .concat(app.eachChange(inputNode))
                        .take(1)
                        .takeUntil(app.navigate())
                        .map(function ()
                        {
                            return input.files;
                        })
                        .do(function ()
                        {
                            document.body.removeChild(input);
                        });
                }
            });

        })(window.app, window);
    });