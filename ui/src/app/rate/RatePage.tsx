import * as React from "react";
import Page from "../common/Page";
import {Card, CardBody, CardHeader} from "reactstrap";

class RatePage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {
            checked: "",
            matches: [
                {
                    id: 1,
                    text: 'EVO (\'s-Gravenhage), Eigen Vervoerdersorganisatie (\'s-Gravenhage), (distance 0.863636)'
                },
                {id: 2, text: 'EVO, Algemene Verladers- en Eigen Vervoerders Organisatie, (distance 0.875)'},
                {id: '?', text: 'Niet in lijst'}
            ]
        };
    }

    render() {

        return (
            <Page>
                <h2>Link</h2>
                <Card>
                    <CardHeader>
                        <strong>REVO (per)</strong>
                    </CardHeader>
                    <CardBody>
                        [..] Context lorem ipsum <em>REVO (per)</em> dolor sit amet [..]
                    </CardBody>
                </Card>
                <ul className="list-group mt-3">
                    {this.state.matches.map((m: any, i: number) => {
                        return <li
                            key={i}
                            className="list-group-item list-group-item-action"
                        >
                            <div className="custom-control custom-radio">
                                <input
                                    id={`name-${m.id}`}
                                    name="names"
                                    type="radio"
                                    className="custom-control-input"
                                    checked={this.state.checked === `name-${m.id}`}
                                    onChange={() => this.setState({checked: `name-${m.id}`})}
                                />
                                <label
                                    className="custom-control-label w-100"
                                    htmlFor={`name-${m.id}`}
                                >
                                    {m.text}
                                </label>
                            </div>
                        </li>;
                    })}
                </ul>
                <div className="rate-btns float-right mt-3">
                    <button className="btn btn-secondary mr-3">
                        Skip
                        &nbsp;
                        <i className="fa fa-chevron-right"/>
                    </button>
                    <button className="btn btn-success">
                        Opslaan
                        &nbsp;
                        <i className="fa fa-chevron-right"/>
                    </button>
                </div>
            </Page>
        );
    }
}

export default RatePage;