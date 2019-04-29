import * as React from "react";
import Page from "../common/Page";
import {Card, CardHeader} from "reactstrap";
import Resources from "../Resources";
import {withRouter} from "react-router";

class RatePage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {
            checked: null,
            itemWithSuggestions: [],
            loading: true
        };
        this.requestRandomItem();
    }

    private requestRandomItem() {
        Resources.getRandomIndex().then((randomIndexResponse) => {
            randomIndexResponse.text().then((text) => {
                const index = parseInt(text);
                Resources.getItem(index).then((indexResponse) => {
                    indexResponse.json().then((json) => {
                        json.candidates.push({id: '?', names: 'Niet in lijst', distance: 'n.v.t.'});
                        this.setState({index, itemWithSuggestions: json, loading: false});
                    });
                })
            });
        })
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
                            &nbsp;
                            <span className="text-secondary">[afstand {c.distance}]</span>
                        </label>
                    </div>
                </li>;
            })}
        </ul>;
    }

    render() {
        if (this.state.loading) {
            return <Page>
                <div className="text-center"><i className="fa fa-spinner fa-pulse fa-1x fa-fw"/></div>
            </Page>;
        }

        return (
            <Page>
                <h2>Link</h2>
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

export default withRouter(RatePage);