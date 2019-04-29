import React from 'react';
import './App.css';
import Routes from "./Routes";

class App extends React.Component<any, any> {
  render () {
      return (
          <div className="app">
              <Routes/>
          </div>
      );
  }
}

export default App;
