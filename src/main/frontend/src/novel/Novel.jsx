import React from 'react';
import {Link} from "react-router-dom";

function novel(props) {

    return (
        <div>
            <h1>novel</h1>
            <Link to={'/login'}>login</Link><br/>
            <Link to={'/join'}>join</Link><br/>
            <Link to={'/myPage'}>mypage</Link><br/>
            <Link to={'/board'}>board</Link><br/>
        </div>
    )
}

export default novel;