import config from "../config";

class Resources {

    public static getStats = () => {
        return fetch(`${config.HOST}${config.API}/statistics`);
    };

    public static getRandomIndex = () => {
        return fetch(`${config.HOST}${config.API}/randomindex`);
    };

    public static getItem = (index: number) => {
        return fetch(`${config.HOST}${config.API}/items/${index}`);
    };

    public static putAnnotation = (index: number, annotation: string) => {
        return fetch(`${config.HOST}${config.API}/items/${index}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'text/plain',
            },
            body: annotation
        });
    };

    public static getDownload = () => {
        return fetch(`${config.HOST}${config.API}/dump`);
    };

    public static getTerms = (from: number, size: number) => {
        return fetch(`${config.HOST}${config.API}/terms?from=${from}&size=${size}`);
    };

    public static getTerm = (index: number, from: number, size: number) => {
        return fetch(`${config.HOST}${config.API}/terms/${index}?from=${from}&size=${size}`);
    };

}

export default Resources;
