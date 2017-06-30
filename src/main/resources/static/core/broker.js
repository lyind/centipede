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

// subject based observable broker
app.require([
    "lib/Rx.js"
],
function()
{
    console.log("[broker] init");


    // Class Channel implements the broker core subject
    function Channel(subject, isEvent, onDereference)
    {
        // we inherit from Subscriber
        Rx.Subscriber.call(this, this);

        this.onDereference = onDereference;
        this._supplier = undefined;
        this.subscription = undefined;
        this.subject = subject;
        this.cache = (isEvent) ? new Rx.ReplaySubject(1) : new Rx.Subject();

        var thisChannel = this;

        // output observable with added method pull()
        this.observable = this.cache.asObservable()
            // destructor (called after the last subscriber disconnected)
            .finally(function()
            {
                console.log("[broker] destroy: " + thisChannel.subject);

                if (thisChannel.subscription)
                {
                    thisChannel.subscription.unsubscribe();
                }

                if (thisChannel.onDereference)
                {
                    thisChannel.onDereference();
                }
            });

        this.observable = (isEvent) ? this.observable.share() : this.observable.publishReplay(1).refCount();

        // Add pull() property to the returned Observable which allows requesting the next value
        Object.defineProperty(this.observable, "pull", { value: function()
        {
            // un-subscribe from previous source
            if (thisChannel.subscription)
            {
                thisChannel.subscription.unsubscribe();
            }

            var newValues = (thisChannel._supplier) ? thisChannel._supplier() : Rx.Observable.empty();
            thisChannel.subscription = newValues.subscribe(thisChannel);
            if (newValues.pull)
            {
                // allow broker subject to connect to broker subject
                newValues.pull();
            }

            return this;
        }});
    }

    Channel.prototype = Object.create(Rx.Subscriber.prototype);

    Channel.prototype.constructor = Channel;

    Channel.prototype.asObservable = function() { return this.observable; };

    Channel.prototype.setSupplier = function(newSupplier)
    {
        if (typeof newSupplier === 'function')
        {
            this._supplier = newSupplier;
        }
        else
        {
            console.error("[broker] ignore supplier for " + this.subject + ": not a function: ", newSupplier);
            console.trace();
        }
    };

    Channel.prototype.next = function(value)
    {
        this.cache.next(value);
    };

    Channel.prototype.error = function(e)
    {
        console.log("[broker] error for: " + this.subject);
        this.cache.error(e);
    };

    Channel.prototype.complete = function()
    {
        // ignore, we never complete
    };


    (function(app, Rx)
    {
        var store = {};

        // normalizes subject (array to string)
        var normalizeSubject = function(subject)
        {
            if (Array.isArray(subject))
            {
                return subject.join("/");
            }

            return subject;
        };

        // Get an observable for a specific subject.
        // Optionally set a new supplier function (must return an observable).
        // If isEvent is defined, the channel won't cache values (behave like Subject, not ReplaySubject(1)).
        var broker = function(subject, supplier, isEvent)
        {
            subject = normalizeSubject(subject);

            var channel = store[subject];
            if (!channel)
            {
                // create a channel (an observer that never completes and has a ReplaySubject attached to it)
                channel = new Channel(subject, isEvent, function()
                {
                    // un-register from store after last subscriber disconnected
                    delete store[subject];
                });

                store[subject] = channel;
            }

            if (supplier !== undefined && supplier !== null)
            {
                channel.setSupplier((app.isFunction(supplier)) ? supplier : function() { return Rx.Observable.of(supplier); });
            }

            return channel.asObservable();
        };


        // publish
        Object.defineProperty(app, "broker", { value: broker });
        Object.defineProperty(app, "brokerEvent", { value: function(subject, supplier)
        {
            return broker(subject, supplier, true);
        }});

        // broker subject id constants
        Object.defineProperty(app, "subject", { value: {} });

    })(window.app, window.Rx);
});