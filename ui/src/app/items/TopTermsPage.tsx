import * as React from "react";
import Resources from "../Resources";
import LoadingPage from "../common/LoadingPage";
import Page from "../common/Page";
import MinimalPagination from "../common/MinimalPagination";

class TopTermsPage extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {
            loading: true,
            termPageFrom: 0,
            termLastPageNumber: 99,
            termPageSize: 10,
            terms: []
        };
        this.getTopTerms();
    }

    private getTopTerms(from = this.state.termPageFrom, size = this.state.termPageSize) {
        Resources.getTerms(from, size).then((termsResponse) => {
            termsResponse.json().then((json) => {
                this.setState({
                    terms: json,
                    termPageFrom: from,
                    termPageSize: size,
                    loading: false
                });
            });
        }).catch(() => this.setState({loading: false, error: "Could not get terms"}))
    }


    private renderTerms() {
        return <ul className="list-group mt-3">
            {this.state.terms.map((t: any, i: number) => {
                return <li
                    key={i}
                    className="list-group-item list-group-item-action"
                >
                    <div className="custom-control custom-radio">
                        <span className="text-primary">{t.key}</span>
                        <small className="text-secondary">({t.freq}x)</small>
                    </div>
                </li>;
            })}
        </ul>;
    }

    render() {
        if (this.state.loading)
            return <LoadingPage/>;

        let size = this.state.termPageSize;
        let from = this.state.termPageFrom;

        return (
            <Page className="top-items">
                <h2>Persons and Corporations</h2>
                {this.renderTerms()}
                {/* server is zero- and component one-based: */}
                <MinimalPagination
                    page={from / size + 1}
                    lastPage={this.state.termLastPageNumber}
                    onPrevious={() => this.getTopTerms(from - size)}
                    onNext={() => this.getTopTerms(from + size)}
                />
            </Page>
        );
    }
}

export default TopTermsPage;