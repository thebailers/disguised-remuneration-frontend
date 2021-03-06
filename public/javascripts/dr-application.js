/* global $ */
/* global jQuery */
/* global GOVUK */


$(document).ready(function () {
    // Turn off jQuery animation
    jQuery.fx.off = true;
    $(".radios__item").addClass('multiple-choice');

    // Where .multiple-choice uses the data-target attribute
    // to toggle hidden content
    var showHideContentFoo = new GOVUK.ShowHideContentFoo();

    showHideContentFoo.init();

    $(".govuk-radios__item_nino").addClass('multiple-choice');
    $(".govuk-radios__item_utr").addClass('multiple-choice');
    $('input[type=radio][name=aboutyou-identity]').change(function() {
        if (this.value == 'Left') {
            $('#conditional-aboutyou-identity-conditional-1').show();
            $('#conditional-aboutyou-identity-conditional-2').hide();
        }
        else if (this.value == 'Right') {
            $('#conditional-aboutyou-identity-conditional-1').hide();
            $('#conditional-aboutyou-identity-conditional-2').show();
        }
    });


    $('#conditional-aboutyou-identity-conditional-1').hide();
    $('#conditional-aboutyou-identity-conditional-2').hide();

    $('input.volume').keyup(function(event) {

        // format number
        $(this).val(function(index, value) {
            return value
                .replace(/\D/g, "")
                .replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        });
    });


});

function removeSpecialChars( myid ) {
    return myid.replace( /(:|\.|\[|\]|,|=|@)/g, "\\$1" );
}

window.onload = function () {





    function gaWithCallback(send, event, category, action, label, callback) {
        ga(send, event, category, action, label, {
            hitCallback: gaCallback
        });
        var gaCallbackCalled = false;
        setTimeout(gaCallback, 5000);

        function gaCallback() {
            if(!gaCallbackCalled) {
                callback();
                gaCallbackCalled = true;
            }
        }
    }


};