import * as React from "react";
import Page from "../common/Page";
import {Card, CardHeader} from "reactstrap";
import Resources from "../Resources";
import {withRouter} from "react-router";
import LoadingPage from "../common/LoadingPage";
import InfoBox from "../common/InfoBox";
import InfoPage from "../common/InfoPage";

class AnnotatePage extends React.Component<any, any> {
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
                Resources.getItem(index).then((indexResponse) => {
                    indexResponse.json().then((json) => {
                        json.candidates.push({id: '?', names: 'Niet in lijst', distance: 'n.v.t.'});
                        this.setState({index, itemWithSuggestions: json, loading: false});
                    });
                }).catch(() => this.setState({loading: false, error: "Could not get new item"}))
            });
        }).catch(() => this.setState({loading: false, error: "Could not get new random index"}))
    }

    private handleSkip = () => {
        this.props.history.push('/rate/');
    };

    private handleRating = () => {
        Resources.putAnnotation(this.state.index, this.state.checked).then(() => {
            this.props.history.push('/rate/');
        });
    };

    private renderSuggestions() {
        if (this.state.loading)
            return <LoadingPage/>;

        if (this.state.error)
            return <InfoPage msg={this.state.error} type="warning" />;

        return <ul className="list-group mt-3">
            {this.state.itemWithSuggestions.candidates.map((c: any, i: number) => {
                return <li
                    key={i}
                    className="list-group-item list-group-item-action"
                >
                    <div className="custom-control custom-radio">
                        <input
                            id={`name-${c.id}`}
                            name="names"
                            type="radio"
                            className="custom-control-input"
                            checked={this.state.checked === c.id}
                            onChange={() => this.setState({checked: c.id})}
                        />
                        <label
                            className="custom-control-label w-100"
                            htmlFor={`name-${c.id}`}
                        >
                            <span className="text-primary">{c.names}</span>
                            <br />
                            <small className="text-secondary">Afstand: {c.distance}</small>
                        </label>
                    </div>
                </li>;
            })}
        </ul>;
    }

    render() {
        if (this.state.loading)
            return <LoadingPage/>;
        if (this.state.error)
            return <InfoPage msg={this.state.error} type="warning" />

        return (
            <Page>
                <h2>Annotate</h2>
                <Card>
                    <CardHeader>
                        <strong>{this.state.itemWithSuggestions.input}</strong>
                    </CardHeader>
                </Card>
                {this.renderSuggestions()}
                <div className="rate-btns float-right mt-3 mb-3">
                    <button
                        className="btn btn-secondary mr-3"
                        onClick={this.handleSkip}
                    >
                        Overslaan
                        &nbsp;
                        <i className="fa fa-chevron-right"/>
                    </button>
                    <button
                        className="btn btn-success"
                        disabled={this.state.checked === null}
                        onClick={this.handleRating}
                    >
                        Opslaan
                        &nbsp;
                        <i className="fa fa-chevron-right"/>
                    </button>
                </div>
            </Page>
        );
    }
}

export default withRouter(AnnotatePage);