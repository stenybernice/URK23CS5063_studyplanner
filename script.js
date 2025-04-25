document.addEventListener('DOMContentLoaded', () => {
    const timeDisplay = document.getElementById('timeDisplay');
    const start25Btn = document.getElementById('start25Btn');
    const start50Btn = document.getElementById('start50Btn');
    const resetBtn = document.getElementById('resetBtn');
    const addSubjectBtn = document.getElementById('addSubjectBtn');
    const subjectSelect = document.getElementById('subject');
    const historyList = document.getElementById('historyList');

    let timer;
    let remainingTime = 0;
    const sessionHistory = JSON.parse(localStorage.getItem('sessionHistory')) || [];

    // Function to update the timer display
    function updateTimerDisplay() {
        const minutes = Math.floor(remainingTime / 60);
        const seconds = remainingTime % 60;
        timeDisplay.textContent = `${minutes < 10 ? '0' : ''}${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
    }

    // Start the timer for 25 minutes
    start25Btn.addEventListener('click', () => startTimer(25));
    // Start the timer for 50 minutes
    start50Btn.addEventListener('click', () => startTimer(50));
    // Reset the timer
    resetBtn.addEventListener('click', resetTimer);
    // Add a new subject
    addSubjectBtn.addEventListener('click', addSubject);

    // Load session history and subjects
    loadSessionHistory();
    loadSubjects();

    // Start a new session with the given duration in minutes
    function startTimer(minutes) {
        remainingTime = minutes * 60;
        clearInterval(timer);
        updateTimerDisplay();
        timer = setInterval(() => {
            remainingTime--;
            updateTimerDisplay();
            if (remainingTime <= 0) {
                clearInterval(timer);
                saveSession(minutes);
            }
        }, 1000);
    }

    // Reset the timer
    function resetTimer() {
        clearInterval(timer);
        remainingTime = 0;
        updateTimerDisplay();
    }

    // Save session to local storage
    function saveSession(duration) {
        const subject = subjectSelect.value;
        const timestamp = new Date().toLocaleString();
        sessionHistory.push({ subject, duration, timestamp });
        localStorage.setItem('sessionHistory', JSON.stringify(sessionHistory));
        loadSessionHistory();
    }

    // Load session history from local storage
    function loadSessionHistory() {
        historyList.innerHTML = '';
        sessionHistory.forEach(session => {
            const li = document.createElement('li');
            li.textContent = `${session.timestamp} - ${session.subject} (${session.duration} mins)`;
            historyList.appendChild(li);
        });
    }

    // Load subjects from local storage
    function loadSubjects() {
        const subjects = JSON.parse(localStorage.getItem('subjects')) || [];
        subjects.forEach(subject => {
            const option = document.createElement('option');
            option.textContent = subject;
            subjectSelect.appendChild(option);
        });
    }

    // Add new subject
    function addSubject() {
        const newSubject = prompt('Enter a new subject name:');
        if (newSubject) {
            const subjects = JSON.parse(localStorage.getItem('subjects')) || [];
            if (!subjects.includes(newSubject)) {
                subjects.push(newSubject);
                localStorage.setItem('subjects', JSON.stringify(subjects));
                const option = document.createElement('option');
                option.textContent = newSubject;
                subjectSelect.appendChild(option);
            }
        }
    }
});
