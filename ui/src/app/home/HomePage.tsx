import * as React from "react";
import Page from "../common/Page";
import LoadingPage from "../common/LoadingPage";
import Resources from "../Resources";
import {Link} from "react-router-dom";
import InfoBox from "../common/InfoBox";
import * as FileSaver from "file-saver";

class HomePage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {
            stats: null,
            loading: true,
            error: null,
            info: null
        };

        this.getStats();
    }

    private getStats() {
        Resources.getStats().then((data) => {
            data.json().then((json) => this.setState({loading: false, stats: json}));
        }).catch(() => {
            this.setState({loading: false, error: "Could not get stats"});
        });
    }

    private onDownload() {
        this.setState({loading: true}, () => {
            Resources
                .getDownload()
                .then((data) => {
                    data.text().then((txt) => {
                        const blob = new Blob([txt], {type: " application/json;charset=utf-8"});
                        FileSaver.saveAs(blob, "dump.json");
                        this.setState({loading: false, info: "Annotated entities downloaded as dump.json"})
                    });
                })
                .catch(() => this.setState({loading: false, error: "Could not download"}));
        });
    }

    render() {
        if (this.state.loading)
            return <LoadingPage/>;

        let todo = this.state.stats ? this.state.stats.todo : "-";
        let total = this.state.stats ? this.state.stats.done + todo : "-";

        return (
            <Page>
                <InfoBox msg={this.state.error} type="warning" onClose={() => this.setState({error: null})}/>
                <InfoBox msg={this.state.info} type="info" onClose={() => this.setState({info: null})}/>

                <InfoBox msg={<>
                    From&nbsp;
                    <span className="badge badge-secondary badge-pill">{total}</span>&nbsp;
                    items there are&nbsp;
                    <span className="badge badge-secondary badge-pill">{todo}</span>&nbsp;
                    left to annotate.
                </>} type="info"/>

                <ul className="list-group">
                    <li className="list-group-item">
                        View pers- and corpnames:&nbsp;
                        <Link
                            to="/terms/"
                            className="btn btn-success btn-sm float-right"
                        >
                            view
                            &nbsp;
                            <i className="fa fa-chevron-right"/>
                        </Link>
                    </li>
                    <li className="list-group-item">
                        Annotate next random item:&nbsp;
                        <Link
                            to="/annotate/"
                            className="btn btn-success btn-sm float-right"
                        >
                            annotate
                            &nbsp;
                            <i className="fa fa-chevron-right"/>
                        </Link>
                    </li>
                    <li className="list-group-item">
                        Download JSON export of all entities
                        <button
                            onClick={() => this.onDownload()}
                            className="btn btn-success btn-sm float-right"
                        >
                            download
                            &nbsp;
                            <i className="fa fa-download"/>
                        </button>
                    </li>
                </ul>
            </Page>
        );
    }
}

export default HomePage;
