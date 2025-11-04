// Show notification
function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s forwards';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Form validation
function validateForm(formElement) {
    const inputs = formElement.querySelectorAll('input[required], textarea[required]');
    let isValid = true;

    inputs.forEach(input => {
        if (!input.value.trim()) {
            isValid = false;
            input.classList.add('is-invalid');
            const feedback = input.nextElementSibling || document.createElement('div');
            feedback.className = 'invalid-feedback';
            feedback.textContent = 'This field is required';
            if (!input.nextElementSibling) {
                input.parentNode.appendChild(feedback);
            }
        } else {
            input.classList.remove('is-invalid');
            if (input.nextElementSibling?.className === 'invalid-feedback') {
                input.nextElementSibling.remove();
            }
        }
    });

    return isValid;
}

// Image preview
function previewImage(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.querySelector('#imagePreview');
            preview.src = e.target.result;
            preview.style.display = 'block';
        }
        reader.readAsDataURL(input.files[0]);
    }
}

// Countdown timer for events
function initCountdown(eventDate, elementId) {
    const countdownElement = document.getElementById(elementId);
    if (!countdownElement) return;

    function updateCountdown() {
        const now = new Date().getTime();
        const distance = new Date(eventDate).getTime() - now;

        const days = Math.floor(distance / (1000 * 60 * 60 * 24));
        const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((distance % (1000 * 60)) / 1000);

        countdownElement.innerHTML = `
            <div class="countdown-item">${days}d</div>
            <div class="countdown-item">${hours}h</div>
            <div class="countdown-item">${minutes}m</div>
            <div class="countdown-item">${seconds}s</div>
        `;

        if (distance < 0) {
            clearInterval(timer);
            countdownElement.innerHTML = '<div class="event-ended">Event has ended</div>';
        }
    }

    updateCountdown();
    const timer = setInterval(updateCountdown, 1000);
}

// Infinite scroll for events
let page = 1;
let loading = false;

function loadMoreEvents() {
    if (loading) return;
    
    const eventContainer = document.querySelector('.event-container');
    if (!eventContainer) return;

    loading = true;
    const loadingSpinner = document.createElement('div');
    loadingSpinner.className = 'loading-spinner mx-auto mt-4';
    eventContainer.appendChild(loadingSpinner);

    fetch(`/api/events?page=${page}`)
        .then(response => response.json())
        .then(data => {
            loadingSpinner.remove();
            if (data.length > 0) {
                data.forEach(event => {
                    const eventCard = createEventCard(event);
                    eventContainer.appendChild(eventCard);
                });
                page++;
            }
            loading = false;
        })
        .catch(error => {
            console.error('Error loading events:', error);
            loadingSpinner.remove();
            loading = false;
        });
}

// Event card template
function createEventCard(event) {
    const card = document.createElement('div');
    card.className = 'event-card animate-fade-in-up mb-4';
    card.innerHTML = `
        <div class="card gradient-border">
            <div class="event-date">
                <h3 class="mb-0">${formatDate(event.eventDate)}</h3>
            </div>
            <div class="card-body">
                <h5 class="card-title">${event.title}</h5>
                <p class="card-text">${event.description}</p>
                <div class="d-flex justify-content-between align-items-center">
                    <span class="venue"><i class="fas fa-map-marker-alt"></i> ${event.venue}</span>
                    <a href="/events/${event.id}" class="btn btn-gradient-primary">View Details</a>
                </div>
            </div>
        </div>
    `;
    return card;
}

// Date formatting
function formatDate(dateString) {
    const options = { 
        weekday: 'short', 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    };
    return new Date(dateString).toLocaleDateString('en-US', options);
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // Form validation
    const forms = document.querySelectorAll('form[data-validate]');
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            if (!validateForm(this)) {
                e.preventDefault();
            }
        });
    });

    // Image upload preview
    const imageInputs = document.querySelectorAll('input[type="file"][data-preview]');
    imageInputs.forEach(input => {
        input.addEventListener('change', function() {
            previewImage(this);
        });
    });

    // Infinite scroll
    if (document.querySelector('.event-container')) {
        window.addEventListener('scroll', () => {
            if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 1000) {
                loadMoreEvents();
            }
        });
    }

    // Initialize all countdowns
    document.querySelectorAll('[data-countdown]').forEach(element => {
        initCountdown(element.dataset.countdown, element.id);
    });
});