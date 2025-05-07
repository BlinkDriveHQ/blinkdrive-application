document.addEventListener('DOMContentLoaded', function() {
    // Initialize tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Handle file uploads with progress
    const fileUploadForm = document.getElementById('fileUploadForm');
    if (fileUploadForm) {
        fileUploadForm.addEventListener('submit', function(event) {
            const fileInput = document.getElementById('file');
            if (fileInput.files.length === 0) {
                return; // No file selected
            }

            // Show progress indicator
            const progressContainer = document.getElementById('uploadProgressContainer');
            const progressBar = document.getElementById('uploadProgressBar');
            
            if (progressContainer && progressBar) {
                progressContainer.classList.remove('d-none');
                
                // Simulate progress (in a real app, this would use XMLHttpRequest or Fetch API)
                let progress = 0;
                const interval = setInterval(function() {
                    progress += 10;
                    progressBar.style.width = progress + '%';
                    progressBar.setAttribute('aria-valuenow', progress);
                    
                    if (progress >= 100) {
                        clearInterval(interval);
                    }
                }, 300);
            }
        });
    }
    
    // File drag and drop handling
    const dropZone = document.getElementById('dropZone');
    if (dropZone) {
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, preventDefaults, false);
        });
        
        function preventDefaults(e) {
            e.preventDefault();
            e.stopPropagation();
        }
        
        ['dragenter', 'dragover'].forEach(eventName => {
            dropZone.addEventListener(eventName, highlight, false);
        });
        
        ['dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, unhighlight, false);
        });
        
        function highlight() {
            dropZone.classList.add('bg-light');
        }
        
        function unhighlight() {
            dropZone.classList.remove('bg-light');
        }
        
        dropZone.addEventListener('drop', handleDrop, false);
        
        function handleDrop(e) {
            const dt = e.dataTransfer;
            const files = dt.files;
            const fileInput = document.getElementById('file');
            
            if (fileInput && files.length > 0) {
                fileInput.files = files;
                // Update file name display
                const fileNameDisplay = document.getElementById('fileName');
                if (fileNameDisplay) {
                    fileNameDisplay.textContent = files[0].name;
                }
            }
        }
    }
    
    // Handle directory creation validation
    const directoryForm = document.querySelector('form[action="/directory/create"]');
    if (directoryForm) {
        directoryForm.addEventListener('submit', function(event) {
            const directoryNameInput = document.getElementById('directoryName');
            if (directoryNameInput && directoryNameInput.value.trim() === '') {
                event.preventDefault();
                alert('Please enter a directory name');
            }
        });
    }
    
    // Confirm file/directory deletion
    const deleteButtons = document.querySelectorAll('.delete-btn');
    deleteButtons.forEach(button => {
        button.addEventListener('click', function(event) {
            if (!confirm('Are you sure you want to delete this item? This action cannot be undone.')) {
                event.preventDefault();
            }
        });
    });
    
    // File type preview
    const fileInput = document.getElementById('file');
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            const fileNameDisplay = document.getElementById('fileName');
            const fileTypeIcon = document.getElementById('fileTypeIcon');
            
            if (this.files.length > 0) {
                const file = this.files[0];
                
                if (fileNameDisplay) {
                    fileNameDisplay.textContent = file.name;
                }
                
                if (fileTypeIcon) {
                    // Set icon based on file type
                    const fileExtension = file.name.split('.').pop().toLowerCase();
                    let iconClass = 'fa-file';
                    
                    switch (fileExtension) {
                        case 'pdf':
                            iconClass = 'fa-file-pdf';
                            break;
                        case 'doc':
                        case 'docx':
                            iconClass = 'fa-file-word';
                            break;
                        case 'xls':
                        case 'xlsx':
                            iconClass = 'fa-file-excel';
                            break;
                        case 'ppt':
                        case 'pptx':
                            iconClass = 'fa-file-powerpoint';
                            break;
                        case 'jpg':
                        case 'jpeg':
                        case 'png':
                        case 'gif':
                            iconClass = 'fa-file-image';
                            break;
                        case 'zip':
                        case 'rar':
                        case '7z':
                            iconClass = 'fa-file-archive';
                            break;
                        case 'mp3':
                        case 'wav':
                            iconClass = 'fa-file-audio';
                            break;
                        case 'mp4':
                        case 'avi':
                        case 'mov':
                            iconClass = 'fa-file-video';
                            break;
                        case 'html':
                        case 'css':
                        case 'js':
                            iconClass = 'fa-file-code';
                            break;
                        case 'txt':
                            iconClass = 'fa-file-alt';
                            break;
                    }
                    
                    // Update icon class
                    fileTypeIcon.className = ''; // Clear existing classes
                    fileTypeIcon.classList.add('fas', iconClass);
                }
            }
        });
    }
    
    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
    
    // Password strength meter for registration
    const passwordInput = document.getElementById('password');
    const passwordStrength = document.getElementById('passwordStrength');
    
    if (passwordInput && passwordStrength) {
        passwordInput.addEventListener('input', function() {
            const password = this.value;
            let strength = 0;
            
            if (password.length >= 8) {
                strength += 1;
            }
            
            if (password.match(/[a-z]/) && password.match(/[A-Z]/)) {
                strength += 1;
            }
            
            if (password.match(/[0-9]/)) {
                strength += 1;
            }
            
            if (password.match(/[^a-zA-Z0-9]/)) {
                strength += 1;
            }
            
            // Update strength meter
            passwordStrength.className = '';
            passwordStrength.classList.add('progress-bar');
            
            switch (strength) {
                case 0:
                    passwordStrength.classList.add('bg-danger');
                    passwordStrength.style.width = '25%';
                    break;
                case 1:
                    passwordStrength.classList.add('bg-danger');
                    passwordStrength.style.width = '25%';
                    break;
                case 2:
                    passwordStrength.classList.add('bg-warning');
                    passwordStrength.style.width = '50%';
                    break;
                case 3:
                    passwordStrength.classList.add('bg-info');
                    passwordStrength.style.width = '75%';
                    break;
                case 4:
                    passwordStrength.classList.add('bg-success');
                    passwordStrength.style.width = '100%';
                    break;
            }
        });
    }
    
    // Sort files and directories
    const sortOptions = document.querySelectorAll('.sort-option');
    sortOptions.forEach(option => {
        option.addEventListener('click', function() {
            const sortBy = this.getAttribute('data-sort');
            const items = document.querySelectorAll('.sortable-item');
            const itemsArray = Array.from(items);
            
            // Sort items
            itemsArray.sort((a, b) => {
                const valueA = a.getAttribute('data-' + sortBy);
                const valueB = b.getAttribute('data-' + sortBy);
                
                if (sortBy === 'size' || sortBy === 'date') {
                    return Number(valueA) - Number(valueB);
                } else {
                    return valueA.localeCompare(valueB);
                }
            });
            
            // Update DOM
            const container = document.querySelector('.sortable-container');
            if (container) {
                itemsArray.forEach(item => {
                    container.appendChild(item);
                });
            }
            
            // Update active sort option
            sortOptions.forEach(o => o.classList.remove('active'));
            this.classList.add('active');
        });
    });
});