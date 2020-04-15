if(window.location.pathname !== "/digital-services-tax/time-out") {
    GOVUK.sessionTimeout({
        timeout: 900,
        countdown: 120,
        keep_alive_url: window.location.href,
        logout_url:'/digital-services-tax/time-out',
        timed_out_url: '/digital-services-tax/time-out'
    })
};