import * as React from "react";
import Page from "../common/Page";
import Resources from "../Resources";
import {withRouter} from "react-router";
import LoadingPage from "../common/LoadingPage";
import InfoPage from "../common/InfoPage";
import Annotator from "./Annotator";

class RandomAnnotatePage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {
            checked: null,
            itemWithSuggestions: [],
            loading: true,
            error: null
        };
        this.getRandomItem();
    }

    private getRandomItem() {
        Resources.getRandomIndex().then((randomIndexResponse) => {
            randomIndexResponse.text().then((text) => {
                const index = parseInt(text);
                this.setState({index, loading: false});
            });
        }).catch(() => this.setState({loading: false, error: "Could not get new random index"}))
    }

    private handleSkip = () => {
        this.props.history.push('/annotate/');
    };

    private handleRating = () => {
        Resources.putAnnotation(this.state.index, this.state.checked).then(() => {
            this.props.history.push('/annotate/');
        }).catch(() => this.setState({loading: false, error: "Could not save new annotation"}));
    };

    render() {
        if (this.state.loading)
            return <LoadingPage/>;

        if (this.state.error)
            return <InfoPage msg={this.state.error} type="warning"/>;

        return (
            <Page>
                <h2>Annotate</h2>
                <Annotator
                    item={this.state.index}
                    checked={this.state.checked}
                    onSetChecked={(checked: string) => this.setState({checked})}
                />
                <div className="rate-btns float-right mt-3 mb-3">
                    <button
                        className="btn btn-secondary mr-3"
                        onClick={this.handleSkip}
                    >
                        skip
                        &nbsp;
                        <i className="fa fa-chevron-right"/>
                    </button>
                    <button
                        className="btn btn-success"
                        disabled={this.state.checked === null}
                        onClick={this.handleRating}
                    >
                        save
                        &nbsp;
                        <i className="fa fa-chevron-right"/>
                    </button>
                </div>
            </Page>
        );
    }
}

export default withRouter(RandomAnnotatePage);