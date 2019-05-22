import * as React from "react";
import {Card, CardBody, CardHeader} from "reactstrap";
import Resources from "../Resources";
import Loader from "../common/Loader";
import InfoBox from "../common/InfoBox";

interface AnnotatorProps {
    item: number;
    checked: boolean;
    onSetChecked: Function;
}

class Annotator extends React.Component<AnnotatorProps, any> {

    constructor(props: AnnotatorProps, context: any) {
        super(props, context);
        this.state = {
            loading: true
        };
        this.getItemWithSuggestions();
    }

    private getItemWithSuggestions() {
        Resources.getItem(this.props.item).then((indexResponse) => {
            indexResponse.json().then((json) => {
                json.candidates.push({id: '?', names: 'Niet in lijst', distance: 'n.v.t.'});
                this.setState({candidates: json.candidates, contextId: json.id, input: json.input, loading: false});
            });
        }).catch(() => this.setState({loading: false, error: "Could not get new item"}));

    }

    private renderSuggestions() {
        return <ul className="list-group mt-3">
            {this.state.candidates.map((c: any, i: number) => {

                let names = Array.isArray(c.names)
                    ? c.names.join(', ')
                    : c.names;

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
                            checked={this.props.checked === c.id}
                            onChange={() => this.props.onSetChecked(c.id)}
                        />
                        <label
                            className="custom-control-label w-100"
                            htmlFor={`name-${c.id}`}
                        >
                            <span className="text-primary">{names}</span>
                            <br/>
                            <small className="text-secondary">Afstand: {c.distance}</small>
                        </label>
                    </div>
                </li>;
            })}
        </ul>;
    }

    render() {
        if(this.state.loading)
            return <Loader />;

        return (
            <div className="annotator">
                <InfoBox msg={this.state.error} type="warning" onClose={() => this.setState({error: null})}/>
                <Card>
                    <CardHeader>
                        <strong>{this.state.input}</strong>
                    </CardHeader>
                    <CardBody>
                        <div className="input-group mb-3">
                            <div className="input-group-prepend">
                                <span
                                    className="input-group-text"
                                    id="context-id"
                                >
                                    Context ID
                                </span>
                            </div>
                            <input
                                type="text"
                                className="form-control"
                                aria-describedby="context-id"
                                defaultValue={this.state.contextId}
                            />
                        </div>
                    </CardBody>
                </Card>
                {this.renderSuggestions()}
            </div>
        );
    }
}

export default Annotator;