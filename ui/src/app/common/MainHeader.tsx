import * as React from "react";
import {Link} from "react-router-dom";
import './MainHeader.css';

class MainHeader extends React.Component<any, any> {
    constructor(props: any, context: any) {
        super(props, context);
        this.state = {};
    }

    render() {
        return (
            <div className="main-header">
                <h1><Link to="/">Evidence</Link></h1>
                <div className="main-nav text-center">
                    <Link className="nav-item" to={"/rate/"}>rate</Link>
                </div>
            </div>
        );
    }
}

export default MainHeader;