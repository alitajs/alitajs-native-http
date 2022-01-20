import { IonApp, IonRouterOutlet, setupIonicReact } from '@ionic/react';
import { Route } from 'react-router-dom';
import { IonReactRouter } from '@ionic/react-router';
import React from 'react';
import ReactDOM from 'react-dom';
import App from './App';
import TestPage from './Test';

/* Core CSS required for Ionic components to work properly */
import '@ionic/react/css/core.css';

/* Basic CSS for apps built with Ionic */
import '@ionic/react/css/normalize.css';
import '@ionic/react/css/structure.css';
import '@ionic/react/css/typography.css';

/* Optional CSS utils that can be commented out */
import '@ionic/react/css/padding.css';
import '@ionic/react/css/float-elements.css';
import '@ionic/react/css/text-alignment.css';
import '@ionic/react/css/text-transformation.css';
import '@ionic/react/css/flex-utils.css';
import '@ionic/react/css/display.css';

import './common.css';

setupIonicReact({
  mode: 'ios',
});

ReactDOM.render(
  <React.StrictMode>
    <IonApp>
      <IonReactRouter>
        <IonRouterOutlet>
          <Route path="/" component={TestPage} />
          <Route path="/app" component={App} />
        </IonRouterOutlet>
      </IonReactRouter>
    </IonApp>
  </React.StrictMode>,
  document.getElementById('root'),
);
