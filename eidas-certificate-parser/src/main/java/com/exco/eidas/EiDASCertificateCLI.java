package com.exco.eidas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.Callable;

import org.bouncycastle.operator.OperatorCreationException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

/*
 * 
 * Example: 
 * 	show --cert=../doc/certificates/cert.pem --passphrase=Welcome123
 */
@Command(name="show",description = "show contents of the certificate")
 class Show  implements Callable<Void>{
    @Option(names = "--cert", required = true, description = "certificate file")
    String certFile;

    @Override
    public Void call() throws IOException {
    	
    	

		// ----------------------------------------------
		EiDASCertificate eidascert = new EiDASCertificate();

		
		String certPem = new String(Files.readAllBytes(Paths.get( certFile )));

		X509Certificate cert = eidascert.getCertificate( certPem );

		
		System.out.println( eidascert.showPem( cert ) );
		
      
      return null;
    }
}

/*
 * 
 * Example: 
 * 	create --json=../doc/certificates/cert.json --passphrase=Welcome123
 */
@Command(name="create", description = "create iedas certificate from json file definition; generate private key with or without password")
 class Create  implements Callable<Void>{
    @Option(names = "--json", required = true, description = "json definition of certificate")
    String jsonFile;
    

    @Option(names = "--cert", required = false, description = "optional to save certificate into file in pem format")
    String certFile;

    @Option(names = "--key", required = false, description = "optional to save private key into file in pem format")
    String keyFile;


    @Option(names = "--passphrase", required = false, description = "passphrase as an argument")
    String passphrase;

    @Option(names = "--passphrase:prompt", required = false, description = "ask passphrase inteactively", interactive = true)
    String passphrasePrompt;

    @Option(names = "--passphrase:env", required = false, description = "env variable that contains passphrase")
    String passphraseEnv;
   
    
    @Override
    public Void call() throws IOException, OperatorCreationException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException {

    	passphrase = EiDASCertificateCLI.getPassphaseByPrecedence( passphrase, passphrasePrompt, passphraseEnv );
    	

		String json = new String(Files.readAllBytes(Paths.get( jsonFile )));

		
		EiDASCertificate eidascert = new EiDASCertificate();
		
		KeyPair keyPair = eidascert.genKeyPair();
		
		
		String keyPem = eidascert.privateKeyPem( keyPair, passphrase );
		
		EiDASCertificateCLI.outputStringToFile( keyFile, keyPem );
		
		
		X509Certificate cert = eidascert.createFromJson(json, keyPair);
		
		String certPem = eidascert.writePem( cert );

		EiDASCertificateCLI.outputStringToFile( certFile, certPem );


		return null;
    }
}
	
/*
 * 
 * Example: 
 * 	set --cert=../doc/certificates/gennedcert.pem --key=../doc/certificates/key.pem --passphrase=Welcome123 --roles=PSP_PI --ncaname=ncaname --ncaid=ncaid
 */
@Command(name="set", description = "set psd2 attributes of the iedas certificate")
 class Set  implements Callable<Void>{
    @Option(names = "--cert", required = true, description = "certificate file in pem format")
    String certFile;

    @Option(names = "--cert:out", required = false, description = "optional file to store certificate")
    String certFileOut;

    @Option(names = "--key", required = true, description = "private key file in pem format")
    String keyFile;

    @Option(names = "--organizationidentifier", required = false, description = "organization identifier as per 5.2.1")
    String orgId;

    @Option(names = "--ncaname", required = false, description = "name of the competent authority")
    String ncaName;

    @Option(names = "--ncaid", required = false, description = "competent authority abbreviated unique identifier")
    String ncaId;

    @Option(names = "--roles", required = true, split = ",", description = "eidas psd roles separated by ,: PSP_AS,PSP_PI,PSP_AI,PSP_IC")
    List<String> roles;
    
    String psdAttrs;
        

    @Option(names = "--passphrase", required = false, description = "passphrase as an argument")
    String passphrase;

    @Option(names = "--passphrase:prompt", required = false, description = "ask passphrase inteactively", interactive = true)
    String passphrasePrompt;

    @Option(names = "--passphrase:env", required = false, description = "env variable that contains passphrase")
    String passphraseEnv;
   
    
    @Override
    public Void call() throws IOException {
    	
    	passphrase = EiDASCertificateCLI.getPassphaseByPrecedence( passphrase, passphrasePrompt, passphraseEnv );
      
		EiDASCertificate eidascert = new EiDASCertificate();

		String cert = new String(Files.readAllBytes(Paths.get( certFile )));
	 
		String key = new String(Files.readAllBytes(Paths.get( keyFile )));
		 
		String certPem = eidascert.addPsdAttibutes( cert, key, passphrase, orgId, ncaName, ncaId, roles );


		EiDASCertificateCLI.outputStringToFile( certFileOut, certPem );
      
		return null;
    }
}
	
@Command(description = "Utility to show contents of a certificate with eiDAS/PSD2 attributes (including roles) and ability to set them.",
	name = "java -jar ospr.jar", mixinStandardHelpOptions = true, 
	version = "iedaspsd 1.0",
	subcommands = {
			HelpCommand.class,
		    Show.class,
		    Create.class,
		    Set.class}
)
public class EiDASCertificateCLI implements Callable<Void>{
	
	public static String getPassphaseByPrecedence( String passphraseOption, String passphraseEnv, String passphrasePrompt ) {
    	// password precedence
		String passphrase = null;
		
		if( passphraseOption != null ) {
			passphrase = passphraseOption;
			
		} else if ( passphraseEnv != null) {
			passphrase = System.getenv( passphraseEnv );
			
		} else if ( passphrasePrompt != null) {
			passphrase = passphrasePrompt;
			
		}
		return passphrase;
	}
    
	public static void outputStringToFile( String file, String contents ) throws IOException {
		if( file == null ) {
			System.out.println( contents );
		}else {
			Files.write( Paths.get( file ) , contents.getBytes() );
		}
	}
	
    public static void main(String[] args) throws Exception {


  	CommandLine commandLine = new CommandLine(new EiDASCertificateCLI());
    	
  	
  	
    	commandLine.parseWithHandler(new CommandLine.RunLast(), args);
    }
    
    @Override
    public Void call() {
      	CommandLine.usage(this, System.err);
		return null;
    }
}
