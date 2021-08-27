var LOGIN_MODE = {
        'AUTOMATIC': "auto"
    },
    autoLogin = Prefab.loginmode === LOGIN_MODE.AUTOMATIC;
Prefab.isInsideIframe = window.self != window.top;


/*
 * Use App.getDependency for Dependency Injection
 * eg: var DialogService = App.getDependency('DialogService');
 */

/*
 * This function will be invoked when any of this prefab's property is changed
 * @key: property name
 * @newVal: new value of the property
 * @oldVal: old value of the property
 */

Prefab.onReady = function() {
    Prefab.login = login;
};

function propertyChangeHandler(key, newVal, oldVal) {
    /*
               switch (key) {
               case "prop1":
               // do something with newVal for property 'prop1'
               break;
               case "prop2":
               // do something with newVal for property 'prop2'
               break;
               }
               */

}
/* register the property change handler */

Prefab.onPropertyChange = propertyChangeHandler;

Prefab.onReady = function() {
    // this method will be triggered post initialization of the prefab.
};
Prefab.signinBtnClick = function($event, widget) {
    openWindow(Prefab.loginurl);
};


Prefab.AzureGetLoginURLonSuccess = function(variable, data) {
    Prefab.loginurl = data && data.value ? data.value : '';
    Prefab.onLoginurlfetch(null, {
        'loginurl': Prefab.loginurl
    });
    toggleSigninBtn(!Prefab.accesstoken);
    if (autoLogin && !Prefab.accesstoken) {
        openWindow(Prefab.loginurl);
    }
};


function toggleSigninBtn(enable) {
    var signInBtn = Prefab.Widgets.signinBtn.$element;
    if (!enable) {
        signInBtn.attr('disabled', 'disabled');
    } else {
        signInBtn.removeAttr('disabled');
    }
}

function login() {
    Prefab.Widgets.signinBtn.$element.click();
}


function openWindow(href) {
    if (!href) {
        return;
    }
    window.open(href, Prefab.isInsideIframe ? '_blank' : '_self', "width=600,height=700");
}

Prefab.AzureGetAccessTokenonSuccess = function(variable, data) {
    var callbackParams;
    if (data && data.value) {
        Prefab.onAccesstokenfetch(null, {
            'accesstoken': Prefab.accesstoken
        });
        Prefab.Widgets.signinBtn.disabled = true;
        toggleSigninBtn(false);
        if (Prefab.loginsuccessmessage) {
            Prefab.Actions.loginSuccess.invoke();
        }
    } else {
        if (Prefab.isInsideIframe) {
            callbackParams = JSON.stringify({
                'WMOAuthState': true
            });
        } else {
            callbackParams = "csrf9999";
        }
        Prefab.Variables.AzureGetLoginURL.setInput('state', callbackParams);
        Prefab.Variables.AzureGetLoginURL.invoke();
    }
};