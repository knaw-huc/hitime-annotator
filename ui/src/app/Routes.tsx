import * as React from "react";
import {Redirect, Route, Switch, withRouter} from "react-router-dom";
import AnnotatePage from "./annotate/AnnotatePage";
import HomePage from "./home/HomePage";
import TopTermsPage from "./items/TopTermsPage";
import TermItemsPage from "./items/TermItemsPage";

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
                <Redirect exact from="/" to="/home/"/>
                <Route exact path='/annotate/' component={AnnotatePage} key={Math.random()}/>
                <Route exact path='/home/' component={HomePage} key={pathname}/>
                <Route exact path='/terms/' component={TopTermsPage} key={pathname}/>
                <Route exact path='/terms/:tid/' component={TermItemsPage} key={pathname}/>
            </Switch>
        );
    }
}

export default withRouter(Routes);