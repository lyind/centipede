// window.saveAs polyfill
// @author Andrew Dodson, Jonas Zeiger
// @copyright MIT, BSD. Free to clone, modify and distribute for commercial and personal use.
"use strict";

(function(window, document)
{
    if (!window.saveAs)
    {
        var saveAs = undefined;
        if (window.navigator.msSaveBlob)
        {
            saveAs = function (blob, name)
            {
                return window.navigator.msSaveBlob(blob, name);
            };
        }
        else if (window.webkitSaveAs)
        {
            saveAs = window.webkitSaveAs;
        }
        else if (window.mozSaveAs)
        {
            saveAs = window.mozSaveAs;
        }
        else if (window.msSaveAs)
        {
            saveAs = window.msSaveAs;
        }
        else
        {
            // supply polyfill
            saveAs = (function ()
            {
                var urlImpl = window.URL || window.webkitURL;
                if (!urlImpl)
                {
                    return undefined;
                }

                return function (blob, name)
                {
                    var dataUrl = urlImpl.createObjectURL(blob);

                    // Test for download link support
                    var a = document.createElement("a");
                    if ("download" in a)
                    {
                        a.setAttribute("hidden", "");
                        a.setAttribute("href", dataUrl);
                        a.setAttribute("download", name);
                        document.body.appendChild(a);

                        a.click();

                        a.parentNode.removeChild(a);
                    }
                    else
                    {
                        // failover, open resource in new tab.
                        window.open(dataUrl, "_blank", "");
                    }

                    if (urlImpl.revokeObjectURL)
                        urlImpl.revokeObjectURL(dataUrl);
                };
            })();
        }

        if (saveAs)
        {
            Object.defineProperty(window, "saveAs", {value: saveAs});
        }
    }
})(window, document);