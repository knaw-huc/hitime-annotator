import * as React from "react";
import LoadingPage from "../common/LoadingPage";
import InfoPage from "../common/InfoPage";
import Page from "../common/Page";
import MinimalPagination from "../common/MinimalPagination";
import Resources from "../Resources";

class TermItemsPage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {
            loading: true,
            from: 0,
            total: 0,
            size: 10,
            items: []
        };
        this.getTermItems();
    }

    private getTermItems(from = this.state.from, size = this.state.size) {
        Resources.getTerm(this.props.match.params.tid, from, size).then((termsResponse) => {
            termsResponse.json().then((json) => {
                this.setState({
                    items: json.occurences,
                    from: from,
                    size: size,
                    total: json.total,
                    loading: false
                });
            });
        }).catch(() => this.setState({loading: false, error: "Could not get terms"}))
    }

    private renderItems() {
        return <ul className="list-group mt-3">
            {this.state.items.map((t: any, i: number) => {
                return <li
                    key={i}
                    className="list-group-item list-group-item-action"
                >
                    <div className="custom-control custom-radio">
                        <span className="text-primary">{t.source}</span>
                        {t.controlAccess ? <small className="text-secondary"> (control access)</small> : null}
                        <button
                            className="btn btn-success btn-sm float-right"
                            onClick={() => this.props.history.push(`/terms/${this.props.match.params.tid}/items/${t.id}/annotate/`)}
                        >
                            annotate
                            &nbsp;
                            <i className="fa fa-chevron-right"/>
                        </button>
                    </div>
                </li>;
            })}
        </ul>;
    }

    render() {
        if (this.state.loading)
            return <LoadingPage/>;

        if (this.state.error)
            return <InfoPage msg={this.state.error} type="warning"/>;

        let size = this.state.size;
        let from = this.state.from;

        return (
            <Page className="term-items">
                <h2>Occurrences of {this.props.match.params.tid}</h2>
                {this.renderItems()}
                {/* server is zero- and component one-based: */}
                <MinimalPagination
                    page={from / size + 1}
                    lastPage={Math.ceil(this.state.total / size)}
                    onPrevious={() => this.getTermItems(from - size)}
                    onNext={() => this.getTermItems(from + size)}
                />
            </Page>
        );
    }
}

export default TermItemsPage;