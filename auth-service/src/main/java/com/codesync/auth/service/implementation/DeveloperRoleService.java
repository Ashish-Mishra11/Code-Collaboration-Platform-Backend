package com.codesync.auth.service.implementation;

import java.io.IOException;
import java.lang.classfile.instruction.ReturnInstruction;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codesync.auth.dto.RegisterDeveloperDto;
import com.codesync.auth.dto.RegisterResponseDto;
import com.codesync.auth.entity.DeveloperApplicationReceived;
import com.codesync.auth.repository.DeveloperApplication;
import com.codesync.auth.repository.UserRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DeveloperRoleService {
	
	private DeveloperApplication developerApplication;
//	private UserRepository userRepository;
    private JavaMailSender mailSender;
	public String registerDeveloperApplications(RegisterDeveloperDto registerDeveloperDto) {
		
		 if(developerApplication.findByEmail(registerDeveloperDto.getEmail()).isPresent()) {
			 return "Your Application already exists in the database. Please wait for admin approval.";
		 }
		
		DeveloperApplicationReceived entity=new DeveloperApplicationReceived();
		entity.setName(registerDeveloperDto.getName());
		entity.setEmail(registerDeveloperDto.getEmail());
        // Validate file
        MultipartFile resume = registerDeveloperDto.getResume();

        if (resume.isEmpty()) {
            return "Resume file is empty"; 
        }

        if (!"application/pdf".equals(resume.getContentType())) {
            return "Only PDF files are allowed";
        }

		try {
			entity.setResumePdf(resume.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Problem with resume pdf part");
		}
		entity.setCompanyName(registerDeveloperDto.getCompanyName());
		
		developerApplication.save(entity);
		
		generateMailToAdmin(registerDeveloperDto);
		return "Your Application Registered successfully now wait for Admin to Authenticate your Application";
	}
	
	
	public void generateMailToAdmin(RegisterDeveloperDto registerDeveloperDto) {
		 try {
		        String adminEmail = "am3410352@gmail.com";

		        MimeMessage mimeMessage = mailSender.createMimeMessage();
		        //needed for attachedment mail
		        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

		        helper.setTo(adminEmail);
		        helper.setSubject("New Developer Application Received");

		        String approveLink = "http://localhost:3000/admin/approve?email=" + registerDeveloperDto.getEmail();
		        String rejectLink = "http://localhost:3000/admin/reject?email=" + registerDeveloperDto.getEmail();

		        String emailBody = "Hello Admin,\n\n"
		                + "A new developer application has been submitted.\n\n"
		                + "Name: " + registerDeveloperDto.getName() + "\n"
		                + "Email: " + registerDeveloperDto.getEmail() + "\n"
		                + "Company: " + registerDeveloperDto.getCompanyName() + "\n\n"
		                + "Choose an action:\n\n"
		                + "Approve: " + approveLink + "\n"
		                + "Reject: " + rejectLink + "\n\n"
		                + "Regards,\nCodeSync System";

		        helper.setText(emailBody);

		        // Attach resume
		        helper.addAttachment(registerDeveloperDto.getResume().getOriginalFilename(), registerDeveloperDto.getResume());

		        mailSender.send(mimeMessage);

		    } catch (Exception e) {
		        throw new RuntimeException("Failed to send email", e);
		    }
	
	}
	public String generateMailToDeveloper(String to) {
	    String registrationLink = "http://localhost:3000/register/developer/complete?email=" + to;

	    SimpleMailMessage message = new SimpleMailMessage();

	    message.setTo(to);
	    message.setSubject("Congratulations! You are Selected as Developer at CodeSync 🎉");

	    String body = "Dear Developer,\n\n"
	            + "Congratulations! 🎉\n\n"
	            + "You have been selected as a Developer for CodeSync.\n\n"
	            + "To complete your registration and set your password, please click the link below:\n\n"
	            + registrationLink + "\n\n"
	            + "This link will allow you to create your account securely.\n\n"
	            + "If you did not apply, please ignore this email.\n\n"
	            + "Welcome aboard! 🚀\n\n"
	            + "Best Regards,\n"
	            + "CodeSync Team";

	    message.setText(body);

	    mailSender.send(message);
		
	    return "mail send to the selected developer";
		
		
	}


	public String rejectHandler(String email) {
		// TODO Auto-generated method stub
		if(developerApplication.findByEmail(email).isPresent()) {
			DeveloperApplicationReceived orElseThrow = developerApplication.findByEmail(email).orElseThrow(()->new RuntimeException("developer not found in rejecthandler"));
			developerApplication.delete(orElseThrow);
			return "The Developer rejected by admin is deleted from database";
		}
		return "No Developer deleted from database";
	}

}
