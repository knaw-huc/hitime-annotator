import * as React from "react";
import {Redirect, Route, Switch, withRouter} from "react-router-dom";
import RatePage from "./rate/RatePage";

class Routes extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {};
    }

    render() {
        // add pathname as key to force instantiation of new component when path changes
        const pathname = this.props.location.pathname;

        return (
            <Switch>
                <Redirect exact from="/" to="/rate/"/>
                <Route exact path='/rate/' component={RatePage} key={pathname}/>
            </Switch>
        );
    }
}

export default withRouter(Routes);