import * as React from "react";
import './MinimalPagination.css';

interface MinimalPaginationProps {
    page: number,
    lastPage: number,
    onPrevious: Function,
    onNext: Function
}

/**
 * Navigate from page 1 to this.props.lastPage
 */
class MinimalPagination extends React.Component<MinimalPaginationProps, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {};
    }

    render() {
        const onFirstPage = this.props.page <= 1;
        const onLastPage = this.props.page >= this.props.lastPage;

        return (
            <div className="search-pagination">
                <nav>
                    <ul className="pagination">
                        <li className="page-item">
                            <button
                                className={`page-link ${onFirstPage ? "disabled" : ""}`}
                                onClick={() => this.props.onPrevious()}
                                disabled={onFirstPage}
                            >
                                Vorige
                            </button>
                        </li>
                        <li className="page-item">
                            <button
                                className="page-link"
                                disabled={true}
                            >
                                {this.props.page}
                            </button>
                        </li>
                        <li className="page-item">
                            <button
                                className={`page-link ${onLastPage ? "disabled" : ""}`}
                                onClick={() => this.props.onNext()}
                                disabled={onLastPage}
                            >
                                Volgende
                            </button>
                        </li>
                    </ul>
                </nav>
            </div>
        );
    }
}

export default MinimalPagination;

