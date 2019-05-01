import * as React from "react";
import Page from "../common/Page";
import LoadingPage from "../common/LoadingPage";
import Resources from "../Resources";
import InfoPage from "../common/InfoPage";
import InfoBox from "../common/InfoBox";
import {Link} from "react-router-dom";

class HomePage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {
            statistics: null,
            loading: true,
            error: null
        };

        this.getStatistics();
    }

    private getStatistics() {
        Resources.getStatistics().then((data) => {
            data.json().then((json) => this.setState({loading: false, statistics: json}));
        }).catch(() => {
            this.setState({loading: false, error: "Could not get statistics"});
        });
    }

    render() {
        if (this.state.loading)
            return <LoadingPage/>;

        if (this.state.error)
            return <InfoPage msg={this.state.error} type="warning"/>;

        return (
            <Page>
                <InfoBox
                    type="info"
                    msg={<>To do: {this.state.statistics.todo} out of {this.state.statistics.done + this.state.statistics.todo} items left to <Link to="/annotate/">annotate <i className="fa fa-chevron-right fa-sm" /></Link></>}
                />
            </Page>
        );
    }
}

export default HomePage;