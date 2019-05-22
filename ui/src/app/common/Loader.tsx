import * as React from "react";

class Loader extends React.Component<any, any> {
    render() {
        return (
            <div className="text-center"><i className="fa fa-spinner fa-pulse fa-1x fa-fw"/></div>
        );
    }
}

export default Loader;

