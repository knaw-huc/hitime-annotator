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
                console.log('json', json);
                let candidates = json.candidates;
                candidates = this.removeDuplicatesById(candidates);
                candidates.push({id: '?', names: 'Not in list', distance: 'n.a.'});
                this.setState({
                    candidates: candidates,
                    contextId: json.id,
                    controlAccess: json.controlaccess,
                    input: json.input,
                    loading: false
                });
            });
        }).catch(() => this.setState({loading: false, error: "Could not get new item"}));

    }

    private removeDuplicatesById(candidates: any) {
        return Array
            .from(new Set(candidates.map((s: any) => s.id)))
            .map((id: any) => {
                return candidates.find((c2: any) => c2.id === id)
            });
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
                        {this.state.controlAccess ? <small className="text-secondary"> (control access)</small> : null}
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