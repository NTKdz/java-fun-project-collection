// npm install basic-ftp
import ftp from "basic-ftp";
import fs from "fs";

async function run() {
    const client = new ftp.Client();
    client.ftp.verbose = false;

    try {
        const start = Date.now();

        await client.access({
            host: "your-ftp-host",
            user: "username",
            password: "password",
            secure: false
        });

        // Upload
        await client.uploadFrom("testfile.dat", "/remote_testfile.dat");

        // Download
        await client.downloadTo("download_testfile.dat", "/remote_testfile.dat");

        const end = Date.now();
        console.log("Total time (ms):", (end - start));
    } catch (err) {
        console.error(err);
    }

    client.close();
}

run();
