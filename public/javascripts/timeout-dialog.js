if(window.location.pathname !== "/digital-services-tax/time-out") {
    $.timeoutDialog({
        timeout: 900,
        countdown: 120,
        keep_alive_url: window.location.href,
        restart_on_yes: true,
        logout_url: '/digital-services-tax/time-out'
    })};