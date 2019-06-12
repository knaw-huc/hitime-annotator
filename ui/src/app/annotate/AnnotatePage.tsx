import * as React from "react";
import Page from "../common/Page";
import Resources from "../Resources";
import {withRouter} from "react-router";
import Annotator from "./Annotator";
import LoadingPage from "../common/LoadingPage";
import InfoBox from "../common/InfoBox";

class AnnotatePage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {
            checked: null,
            input: null,
            itemWithSuggestions: [],
            error: null
        };
    }

    private handleSkip = () => {
        this.props.history.push(`/terms/${this.props.match.params.tid}/`);
    };

    private handleRating = () => {
        Resources.putAnnotation(this.props.match.params.iid, this.state.checked).then(() => {
            let newId = parseInt(this.props.match.params.iid) + 1;
            Resources.getItem(newId).then((response) => {
                if (response.ok) {
                    response.json().then((json) => {
                        this.props.history.push(`/terms/${encodeURIComponent(json.input)}/items/${newId}/annotate`);
                    });
                } else {
                    this.props.history.push(`/terms/`);
                }
            }).catch(() => this.setState({loading: false, error: "Checking if annotation exists failed"}));
        }).catch(() => this.setState({loading: false, error: "Could not save new annotation"}));
    };

    render() {
        if (this.state.loading)
            return <LoadingPage/>;

        return (
            <Page>
                <h2>Annotate</h2>
                {<InfoBox msg={this.state.error} type="warning" onClose={() => this.setState({error: null})}/>}
                <Annotator
                    item={this.props.match.params.iid}
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

export default withRouter(AnnotatePage);